package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 演示 forward：/forward 转到 /hello。
 */
public class ForwardServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        request.getRequestDispatcher("/hello").forward(request, response);
    }
}
