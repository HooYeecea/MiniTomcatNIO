package cn.minitomcatnio;

import cn.minitomcatnio.connector.Connector;
import cn.minitomcatnio.container.Engine;
import cn.minitomcatnio.container.Host;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 启动入口：创建 Engine / Host，扫描 webapps 自动部署后交给 Connector。
 */
public class NioServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        Host host = new Host("localhost");
        host.deployWebapps(Path.of("webapps"));

        Engine engine = new Engine("Catalina");
        engine.setDefaultHost("localhost");
        engine.addHost(host);

        Connector connector = new Connector(PORT, engine);
        connector.start();
    }
}
