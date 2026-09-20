package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

/**
 * 示例 Servlet，映射到 /hello。
 */
public class HelloServlet implements Servlet {

    @Override
    public void init() {
        System.out.println("Servlet init: HelloServlet");
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("Hello Servlet!\nmethod=" + request.getMethod()
                + "\npath=" + request.getPath() + "\n");
    }
}
