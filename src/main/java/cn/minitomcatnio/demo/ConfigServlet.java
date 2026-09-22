package cn.minitomcatnio.demo;

import com.minispring.web.HttpRequest;
import com.minispring.web.HttpResponse;
import com.minispring.web.Servlet;

/**
 * 读取应用级 context-param。
 */
public class ConfigServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        String appName = ((cn.minitomcatnio.http.HttpRequest) request)
                .getContext()
                .getInitParameter("appName");
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("appName=" + appName + "\n");
    }
}
