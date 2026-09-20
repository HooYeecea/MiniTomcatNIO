package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

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
