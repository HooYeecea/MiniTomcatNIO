package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * Context 管道的最后一关：找到 Wrapper 或静态资源。
 */
public class StandardContextValve implements Valve {

    private final Context context;

    public StandardContextValve(Context context) {
        this.context = context;
    }

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveChain chain) {
        context.service(request, response);
    }
}
