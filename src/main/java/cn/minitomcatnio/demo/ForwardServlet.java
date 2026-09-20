package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

/**
 * 演示 forward：/forward 转到 /hello。
 */
public class ForwardServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        request.getRequestDispatcher("/hello").forward(request, response);
    }
}
