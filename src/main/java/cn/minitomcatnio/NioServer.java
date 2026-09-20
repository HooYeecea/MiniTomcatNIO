package cn.minitomcatnio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioServer {

    private static final int PORT = 8080;
    /** HTTP 请求头上限。超过还没看到 \r\n\r\n，就当作非法请求。 */
    private static final int HEADER_BUFFER_SIZE = 8192;

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("NIO HTTP Server started on port " + PORT);

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
     * 数据可能一次到不齐，所以先往 attachment 的 buffer 里攒，
     * 直到出现请求头结束标记 \r\n\r\n。
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

        System.out.println("解析请求: " + request.getMethod() + " " + request.getUri());
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            System.out.println("  " + header.getKey() + ": " + header.getValue());
        }

        String body = "Hello NIO!\n"
                + "method=" + request.getMethod() + "\n"
                + "uri=" + request.getUri() + "\n";
        prepareResponse(key, 200, "OK", body);
    }

    /**
     * 处理写事件（OP_WRITE）。
     * 一次 write 不一定能发完，buffer 还有 remaining 就下次继续写。
     * 发完后关闭连接（这一步先不做 Keep-Alive）。
     */
    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        Connection conn = (Connection) key.attachment();

        clientChannel.write(conn.writeBuffer);

        if (!conn.writeBuffer.hasRemaining()) {
            closeConnection(key);
        }
    }

    private static void prepareResponse(SelectionKey key, int status, String reason, String body) {
        Connection conn = (Connection) key.attachment();
        HttpResponse response = new HttpResponse();
        response.setStatus(status, reason);
        response.setBody(body);
        conn.writeBuffer = response.toByteBuffer();
        key.interestOps(SelectionKey.OP_WRITE);
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
        final ByteBuffer readBuffer = ByteBuffer.allocate(HEADER_BUFFER_SIZE);
        ByteBuffer writeBuffer;
    }

}
