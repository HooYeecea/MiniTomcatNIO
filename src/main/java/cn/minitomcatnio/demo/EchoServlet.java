package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 用来验收参数解析：query string 和 x-www-form-urlencoded 表单。
 */
public class EchoServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        String name = request.getParameter("name");
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("name=" + (name == null ? "" : name) + "\n");
    }
}
