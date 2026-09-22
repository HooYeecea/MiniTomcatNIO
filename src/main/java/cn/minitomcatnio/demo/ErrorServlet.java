package cn.minitomcatnio.demo;

import com.minispring.web.HttpRequest;
import com.minispring.web.HttpResponse;
import com.minispring.web.Servlet;

/**
 * 500 的错误页。状态码由容器在转发后写回。
 */
public class ErrorServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("error page\npath=" + request.getPath() + "\n");
    }
}
