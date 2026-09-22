package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 示例前缀映射：/app/* 。
 */
public class AppServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("App Servlet!\npath=" + request.getPath()
                + "\nservletPath=" + request.getServletPath()
                + "\npathInfo=" + request.getPathInfo() + "\n");
    }
}
