package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

/**
 * 演示 include：先写一行，再嵌入 /hello 的正文。
 */
public class WrapServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("wrapped\n");
        request.getRequestDispatcher("/hello").include(request, response);
    }
}
