package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

/**
 * 调用 sendError(404)，由容器转到 404 的 error-page。
 */
public class MissingServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.sendError(404, "Not Found");
    }
}
