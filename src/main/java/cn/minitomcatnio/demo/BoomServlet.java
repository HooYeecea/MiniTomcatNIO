package cn.minitomcatnio.demo;

import com.minispring.web.HttpRequest;
import com.minispring.web.HttpResponse;
import com.minispring.web.Servlet;

/**
 * 故意抛异常，用来触发 error-page。
 */
public class BoomServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        throw new IllegalStateException("boom");
    }
}
