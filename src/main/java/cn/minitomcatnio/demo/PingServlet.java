package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 第二个应用 /other 的示例 Servlet。
 */
public class PingServlet implements Servlet {

    @Override
    public void init() {
        System.out.println("Servlet init: PingServlet");
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("pong\ncontext=" + request.getContextPath()
                + "\npath=" + request.getPathWithinContext() + "\n");
    }
}
