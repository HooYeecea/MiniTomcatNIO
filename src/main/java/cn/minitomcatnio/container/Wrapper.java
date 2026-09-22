package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import com.web.Servlet;

/**
 * 包住一个 Servlet。Context 的 Mapper 找到的是 Wrapper，再由它去调 Servlet。
 */
public class Wrapper {

    private final String name;
    private final Servlet servlet;

    public Wrapper(String name, Servlet servlet) {
        this.name = name;
        this.servlet = servlet;
    }

    public String getName() {
        return name;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public void invoke(HttpRequest request, HttpResponse response) throws Exception {
        servlet.service(request, response);
    }
}
