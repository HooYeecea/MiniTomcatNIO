package cn.minitomcatnio;

/**
 * 最小 Servlet：一次请求进来，自己往 HttpResponse 里写。
 */
public interface Servlet {

    void service(HttpRequest request, HttpResponse response);
}
