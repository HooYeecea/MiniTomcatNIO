package cn.minitomcatnio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    public static void main(String[] args) throws IOException {
        // 1. 打开 ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        // 2. 设置非阻塞（NIO 的关键一步）
        serverChannel.configureBlocking(false);

        // 3. 绑定端口
        serverChannel.bind(new InetSocketAddress(8080));

        // 4. 打开 Selector
        Selector selector = Selector.open();

        // 5. 把 serverChannel 注册到 Selector，关注"接受连接"事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("NIO Server started on port 8080");

        // 6. 事件循环
        while (true) {
            // 阻塞，直到至少有一个事件发生
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();  // 必须移除，否则下次 select 还会返回它

                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key);
                }
            }
        }
    }

    /**
     * 处理新连接（OP_ACCEPT 事件）
     */
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();  // 不会阻塞

        if (clientChannel == null) {
            return;
        }

        System.out.println("新连接: " + clientChannel.getRemoteAddress());

        // 新连接也要非阻塞，并注册到 Selector，关注"读"事件
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    /**
     * 处理读事件（OP_READ 事件）
     */
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // -1 表示客户端关闭了连接
            System.out.println("连接关闭: " + clientChannel.getRemoteAddress());
            clientChannel.close();
            key.cancel();
            return;
        }

        if (bytesRead == 0) {
            // 没有数据可读（非阻塞模式下可能出现）
            return;
        }

        // 切换 buffer 到"读模式"：position=0, limit=bytesRead
        buffer.flip();

        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        String message = new String(data);
        System.out.println("收到数据 [" + bytesRead + " 字节]:");
        System.out.println(message);
    }
}
