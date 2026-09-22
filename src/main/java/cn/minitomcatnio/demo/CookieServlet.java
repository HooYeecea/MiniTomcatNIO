package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Servlet;

/**
 * 用来验收 Cookie 读写：读 Cookie: name=...，并回 Set-Cookie。
 */
public class CookieServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        String name = request.getCookie("name");
        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("cookie.name=" + (name == null ? "" : name) + "\n");
        response.addCookie("visit", "1");
    }
}
