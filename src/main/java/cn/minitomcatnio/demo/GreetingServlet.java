package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import cn.minitomcatnio.servlet.GenericServlet;

/**
 * 从 web.xml 的 init-param 读取 greeting。
 */
public class GreetingServlet extends GenericServlet {

    @Override
    public void init() {
        System.out.println("Servlet init: GreetingServlet greeting=" + getInitParameter("greeting"));
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("greeting=" + getInitParameter("greeting") + "\n");
    }
}
