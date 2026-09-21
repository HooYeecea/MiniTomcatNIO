package cn.minitomcatnio.demo;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;

/**
 * 故意抛异常，用来触发 error-page。
 */
public class BoomServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        throw new IllegalStateException("boom");
    }
}
