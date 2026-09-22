package cn.minitomcatnio.servlet;

import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.loader.StaticResourceProcessor;
import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 映射到 / 的默认 Servlet。精确、前缀、扩展名都未命中时，由它提供静态文件。
 */
public class DefaultServlet implements Servlet {

    private final Context context;

    public DefaultServlet(Context context) {
        this.context = context;
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        StaticResourceProcessor.process(
                (cn.minitomcatnio.http.HttpRequest) request,
                (cn.minitomcatnio.http.HttpResponse) response,
                context);
    }
}
