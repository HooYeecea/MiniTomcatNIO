package cn.minitomcatnio;

/**
 * 把请求 body 原样写回，用来验收 Content-Length 读取。
 */
public class EchoServlet implements Servlet {

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        String contentType = request.getHeader("content-type");
        response.setStatus(200, "OK");
        response.setHeader("Content-Type",
                contentType != null ? contentType : "text/plain; charset=UTF-8");
        response.setBody(request.getBody());
    }
}
