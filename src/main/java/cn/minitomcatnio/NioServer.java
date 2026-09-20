package cn.minitomcatnio;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 启动入口：搭好 Engine / Host / Context，交给 Connector 监听。
 */
public class NioServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        Context context = new Context(Path.of("webapps", "ROOT"));
        Context other = new Context(Path.of("webapps", "other"));

        Host host = new Host("localhost");
        host.addContext("", context);
        host.addContext("/other", other);

        Engine engine = new Engine("Catalina");
        engine.setDefaultHost("localhost");
        engine.addHost(host);

        Connector connector = new Connector(PORT, engine);
        connector.start();
    }
}
