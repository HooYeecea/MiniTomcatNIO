package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * Host 管道最后一关：按 URI 选出 Context，再交给它。
 */
public class StandardHostValve implements Valve {

    private final Host host;

    public StandardHostValve(Host host) {
        this.host = host;
    }

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveChain chain) {
        Context context = host.findContext(request.getPath());
        if (context == null) {
            response.setStatus(404, "Not Found");
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            response.setBody("404 Not Found: no context");
            return;
        }
        context.invoke(request, response);
    }
}
