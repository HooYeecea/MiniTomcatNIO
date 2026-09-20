package cn.minitomcatnio;

import java.io.IOException;

/**
 * 启动入口：创建一个 Context，交给 Connector 监听。
 */
public class NioServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        Context context = new Context();
        context.addServlet("/hello", new HelloServlet());
        context.addServlet("/echo", new EchoServlet());
        context.addServlet("/app/*", new AppServlet());
        context.addServlet("/cookie", new CookieServlet());
        context.addServlet("/session", new SessionServlet());

        Connector connector = new Connector(PORT, context);
        connector.start();
    }
}
