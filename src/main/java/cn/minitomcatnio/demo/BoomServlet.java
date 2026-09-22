package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 故意抛异常，用来触发 error-page。
 */
public class BoomServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        throw new IllegalStateException("boom");
    }
}
