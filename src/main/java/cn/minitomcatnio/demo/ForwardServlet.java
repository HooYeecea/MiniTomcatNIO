package cn.minitomcatnio.demo;

import com.minispring.web.HttpRequest;
import com.minispring.web.HttpResponse;
import com.minispring.web.Servlet;

/**
 * 演示 forward：/forward 转到 /hello。
 */
public class ForwardServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        request.getRequestDispatcher("/hello").forward(request, response);
    }
}
