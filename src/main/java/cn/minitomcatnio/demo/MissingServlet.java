package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 调用 sendError(404)，由容器转到 404 的 error-page。
 */
public class MissingServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.sendError(404, "Not Found");
    }
}
