package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Engine：容器总入口。按 Host 头选择虚拟主机，没有匹配则走默认 Host。
 */
public class Engine {

    private final String name;
    private final Map<String, Host> hosts = new LinkedHashMap<>();
    private final Pipeline pipeline = new Pipeline();
    private String defaultHost = "localhost";

    public Engine(String name) {
        this.name = name;
        pipeline.setBasic(new StandardEngineValve(this));
    }

    public String getName() {
        return name;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost.toLowerCase();
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public void addHost(Host host) {
        hosts.put(host.getName().toLowerCase(), host);
    }

    public Map<String, Host> hosts() {
        return hosts;
    }

    public void stop() {
        for (Host host : hosts.values()) {
            host.stop();
        }
    }

    public void invoke(HttpRequest request, HttpResponse response) {
        pipeline.invoke(request, response);
    }

    Host findHost(HttpRequest request) {
        String name = hostname(request);
        Host host = hosts.get(name);
        if (host != null) {
            return host;
        }
        return hosts.get(defaultHost);
    }

    private static String hostname(HttpRequest request) {
        String header = request.getHeader("host");
        if (header == null || header.isEmpty()) {
            return "";
        }
        int colon = header.indexOf(':');
        String name = colon >= 0 ? header.substring(0, colon) : header;
        return name.toLowerCase();
    }
}
