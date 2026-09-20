package cn.minitomcatnio;

/**
 * Context 管道的最后一关：Servlet 映射、Session、静态资源。
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
