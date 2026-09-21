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
        Host localhost = new Host("localhost");
        localhost.deployWebapps(Path.of("webapps"));

        Host appLocal = new Host("app.local");
        appLocal.deployWebapps(Path.of("hosts", "app.local"));

        Engine engine = new Engine("Catalina");
        engine.setDefaultHost("localhost");
        engine.addHost(localhost);
        engine.addHost(appLocal);

        Connector connector = new Connector(PORT, engine);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            connector.stop();
            engine.stop();
        }, "nio-shutdown"));
        connector.start();
    }
}
