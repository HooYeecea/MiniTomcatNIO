package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

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
