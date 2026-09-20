package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * Engine 管道最后一关：选出 Host，再交给它。
 */
public class StandardEngineValve implements Valve {

    private final Engine engine;

    public StandardEngineValve(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveChain chain) {
        Host host = engine.findHost(request);
        if (host == null) {
            response.setStatus(404, "Not Found");
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            response.setBody("404 Not Found: no host");
            return;
        }
        host.invoke(request, response);
    }
}
