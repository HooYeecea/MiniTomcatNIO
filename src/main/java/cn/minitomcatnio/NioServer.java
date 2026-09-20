package cn.minitomcatnio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NioServer {

    private static final int PORT = 8080;
    /** HTTP 请求头上限。超过还没看到 \r\n\r\n，就当作非法请求。 */
    private static final int HEADER_BUFFER_SIZE = 8192;
    /** 这一步只按 Content-Length 读 body，先限制大小。 */
    private static final int MAX_BODY_SIZE = 1024 * 1024;
    private static final int WORKER_THREADS = 8;
    private static final Mapper mapper = new Mapper();
    private static final AtomicInteger workerSeq = new AtomicInteger();
    private static final ExecutorService workers = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
        Thread t = new Thread(r);
        t.setName("nio-worker-" + workerSeq.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) throws IOException {
        mapper.addServlet("/hello", new HelloServlet());
        mapper.addServlet("/echo", new EchoServlet());

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("NIO HTTP Server started on port " + PORT);
        System.out.println("workers: " + WORKER_THREADS);
        System.out.println("webroot: " + StaticResourceProcessor.WEB_ROOT);
        mapper.mappings().forEach((path, servlet) ->
                System.out.println("servlet: " + path + " -> " + servlet.getClass().getSimpleName()));

        while (true) {
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (!key.isValid()) {
                    continue;
                }

                try {
                    if (key.isAcceptable()) {
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    System.out.println("连接异常，关闭: " + e.getMessage());
                    closeConnection(key);
                }
            }
        }
    }

    /**
     * 处理新连接（OP_ACCEPT）。
     * 每个连接挂一份 Attachment：读缓冲要跨多次 read 拼请求头。
     */
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel == null) {
            return;
        }

        System.out.println("新连接: " + clientChannel.getRemoteAddress());

        clientChannel.configureBlocking(false);
        Connection conn = new Connection();
        clientChannel.register(selector, SelectionKey.OP_READ, conn);
    }

    /**
     * 处理读事件（OP_READ）。
     * 先拼请求头，再按 Content-Length 把 body 读齐，然后才交给 Worker。
     */
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        Connection conn = (Connection) key.attachment();

        int bytesRead = clientChannel.read(conn.readBuffer);

        if (bytesRead == -1) {
            System.out.println("连接关闭: " + clientChannel.getRemoteAddress());
            closeConnection(key);
            return;
        }

        if (bytesRead == 0) {
            return;
        }

        tryConsumeRequest(key);
    }

    /**
     * 用当前 readBuffer 里已有的字节尝试凑齐一条请求。
     * Keep-Alive 写完后，buffer 里可能已经有下一次请求的数据。
     */
    private static void tryConsumeRequest(SelectionKey key) {
        Connection conn = (Connection) key.attachment();

        if (conn.pendingRequest == null) {
            int headerEnd = HttpRequest.indexOfHeaderEnd(conn.readBuffer);
            if (headerEnd < 0) {
                if (!conn.readBuffer.hasRemaining()) {
                    System.out.println("请求头过大，拒绝");
                    prepareResponse(key, 400, "Bad Request", "Bad Request: headers too large");
                }
                return;
            }

            HttpRequest request = HttpRequest.parse(conn.readBuffer, headerEnd);
            if (request == null) {
                prepareResponse(key, 400, "Bad Request", "Bad Request");
                return;
            }

            int contentLength = request.getContentLength();
            if (contentLength < 0) {
                prepareResponse(key, 400, "Bad Request", "Bad Request: invalid Content-Length");
                return;
            }
            if (contentLength > MAX_BODY_SIZE) {
                prepareResponse(key, 413, "Payload Too Large", "Payload Too Large");
                return;
            }

            System.out.println("解析请求: " + request.getMethod() + " " + request.getUri()
                    + " content-length=" + contentLength);
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                System.out.println("  " + header.getKey() + ": " + header.getValue());
            }

            conn.pendingRequest = request;
            conn.headerEnd = headerEnd;
            conn.contentLength = contentLength;
            ensureReadCapacity(conn, headerEnd + 4 + contentLength);
        }

        int needed = conn.headerEnd + 4 + conn.contentLength;
        if (conn.readBuffer.position() < needed) {
            return;
        }

        HttpRequest request = conn.pendingRequest;
        byte[] body = new byte[conn.contentLength];
        if (conn.contentLength > 0) {
            System.arraycopy(conn.readBuffer.array(), conn.headerEnd + 4, body, 0, conn.contentLength);
        }
        request.setBody(body);

        ByteBuffer buf = conn.readBuffer;
        buf.limit(buf.position());
        buf.position(needed);
        buf.compact();

        conn.pendingRequest = null;
        conn.headerEnd = -1;
        conn.contentLength = 0;

        key.interestOps(0);
        workers.execute(() -> processRequest(key, request));
    }

    private static void ensureReadCapacity(Connection conn, int needed) {
        ByteBuffer buf = conn.readBuffer;
        if (buf.capacity() >= needed) {
            return;
        }
        ByteBuffer grown = ByteBuffer.allocate(needed);
        buf.flip();
        grown.put(buf);
        conn.readBuffer = grown;
    }

    /**
     * Worker 线程：跑 Servlet / 静态资源，再切回 OP_WRITE 让 Selector 发数据。
     */
    private static void processRequest(SelectionKey key, HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            Servlet servlet = mapper.match(request.getPath());
            if (servlet != null) {
                servlet.service(request, response);
            } else {
                StaticResourceProcessor.process(request, response);
            }
        } catch (Exception e) {
            System.out.println("处理请求失败: " + e.getMessage());
            response.setStatus(500, "Internal Server Error");
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            response.setBody("500 Internal Server Error");
        }
        boolean keepAlive = request.shouldKeepAlive();
        response.setHeader("Connection", keepAlive ? "keep-alive" : "close");
        Connection conn = (Connection) key.attachment();
        if (conn != null) {
            conn.keepAlive = keepAlive;
        }
        send(key, response);
    }

    /**
     * 处理写事件（OP_WRITE）。
     * 一次 write 不一定能发完，buffer 还有 remaining 就下次继续写。
     * 发完后：Keep-Alive 则继续读（buffer 里可能已有下一次请求的剩余字节），否则关连接。
     */
    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        Connection conn = (Connection) key.attachment();

        clientChannel.write(conn.writeBuffer);

        if (!conn.writeBuffer.hasRemaining()) {
            if (conn.keepAlive) {
                conn.writeBuffer = null;
                conn.keepAlive = false;
                key.interestOps(SelectionKey.OP_READ);
                if (conn.readBuffer.position() > 0) {
                    tryConsumeRequest(key);
                }
            } else {
                closeConnection(key);
            }
        }
    }

    private static void prepareResponse(SelectionKey key, int status, String reason, String body) {
        Connection conn = (Connection) key.attachment();
        conn.keepAlive = false;
        HttpResponse response = new HttpResponse();
        response.setStatus(status, reason);
        response.setHeader("Connection", "close");
        response.setBody(body);
        send(key, response);
    }

    private static void send(SelectionKey key, HttpResponse response) {
        if (!key.isValid()) {
            return;
        }
        Connection conn = (Connection) key.attachment();
        conn.writeBuffer = response.toByteBuffer();
        try {
            key.interestOps(SelectionKey.OP_WRITE);
            // Worker 改 interest 时 Selector 可能正堵在 select() 里，必须 wakeup。
            key.selector().wakeup();
        } catch (CancelledKeyException ignored) {
        }
    }

    private static void closeConnection(SelectionKey key) {
        try {
            key.channel().close();
        } catch (IOException ignored) {
        }
        key.cancel();
    }

    /** 每个 SocketChannel 自己的读写缓冲，挂在 SelectionKey.attachment 上。 */
    private static class Connection {
        ByteBuffer readBuffer = ByteBuffer.allocate(HEADER_BUFFER_SIZE);
        volatile ByteBuffer writeBuffer;
        volatile boolean keepAlive;
        HttpRequest pendingRequest;
        int headerEnd = -1;
        int contentLength;
    }

}
