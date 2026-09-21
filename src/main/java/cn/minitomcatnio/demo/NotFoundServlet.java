package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

/**
 * 404 的错误页。状态码由容器在转发后写回。
 */
public class NotFoundServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("not found\npath=" + request.getPath() + "\n");
    }
}
