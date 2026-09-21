package cn.minitomcatnio.connector;

import cn.minitomcatnio.container.Engine;
import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

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

/**
 * Connector：NIO 连接、HTTP 解析、Keep-Alive。凑齐一条请求后交给 Engine。
 */
public class Connector {

    private static final int HEADER_BUFFER_SIZE = 8192;
    private static final int MAX_BODY_SIZE = 1024 * 1024;
    private static final int WORKER_THREADS = 8;

    private final int port;
    private final Engine engine;
    private volatile boolean running;
    private volatile Selector selector;
    private volatile ServerSocketChannel serverChannel;
    private final AtomicInteger workerSeq = new AtomicInteger();
    private final ExecutorService workers = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
        Thread t = new Thread(r);
        t.setName("nio-worker-" + workerSeq.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    public Connector(int port, Engine engine) {
        this.port = port;
        this.engine = engine;
    }

    public void start() throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        running = true;

        System.out.println("Connector started on port " + port);
        System.out.println("engine: " + engine.getName() + " defaultHost=" + engine.getDefaultHost());
        System.out.println("workers: " + WORKER_THREADS);
        engine.hosts().forEach((hostName, host) ->
                host.contexts().forEach((contextPath, context) -> {
                    System.out.println("context: [" + hostName + "]"
                            + (contextPath.isEmpty() ? "/" : contextPath)
                            + " docBase=" + context.getDocBase()
                            + " classLoader=" + context.getClassLoader().getClass().getSimpleName());
                    context.mapper().mappings().forEach((path, wrapper) ->
                            System.out.println("servlet: [" + hostName + "]"
                                    + (contextPath.isEmpty() ? "" : contextPath)
                                    + path + " -> "
                                    + wrapper.getServlet().getClass().getSimpleName()));
                }));

        while (running) {
            selector.select();
            if (!running) {
                break;
            }

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
        closeQuietly(selector);
        closeQuietly(serverChannel);
        workers.shutdownNow();
        System.out.println("Connector stopped");
    }

    /** 让事件循环退出。可从其它线程调用。 */
    public void stop() {
        running = false;
        Selector current = selector;
        if (current != null) {
            current.wakeup();
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel == null) {
            return;
        }

        System.out.println("新连接: " + clientChannel.getRemoteAddress());

        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, new Connection());
    }

    private void handleRead(SelectionKey key) throws IOException {
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

    private void tryConsumeRequest(SelectionKey key) {
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

    private void processRequest(SelectionKey key, HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            engine.invoke(request, response);
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

    private void handleWrite(SelectionKey key) throws IOException {
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

    private static class Connection {
        ByteBuffer readBuffer = ByteBuffer.allocate(HEADER_BUFFER_SIZE);
        volatile ByteBuffer writeBuffer;
        volatile boolean keepAlive;
        HttpRequest pendingRequest;
        int headerEnd = -1;
        int contentLength;
    }
}
