package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.HttpSession;
import com.web.Servlet;

/**
 * 用来验收 Session：同一 JSESSIONID 连续访问时 count 递增。
 */
public class SessionServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        HttpSession session = request.getSession();
        Integer count = (Integer) session.getAttribute("count");
        if (count == null) {
            count = 0;
        }
        count++;
        session.setAttribute("count", count);

        response.setStatus(200, "OK");
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody("sessionId=" + session.getId() + "\ncount=" + count + "\n");
    }
}
