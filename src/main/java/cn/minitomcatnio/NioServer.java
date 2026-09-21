package cn.minitomcatnio;

import cn.minitomcatnio.connector.Connector;
import cn.minitomcatnio.container.Engine;
import cn.minitomcatnio.container.Host;

import java.io.IOException;
import java.nio.file.Path;

/**
 * ĺŻĺ¨ĺĽĺŁďźĺĺťş Engine / HostďźćŤć webapps čŞĺ¨é¨ç˝˛ĺäş¤çť Connectoră
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            connector.stop();
            engine.stop();
        }, "nio-shutdown"));
        connector.start();
    }
}
