package cn.minitomcatnio;

/**
 * Servlet 规范里的 RequestDispatcher。这一步只实现 forward。
 */
public interface RequestDispatcher {

    void forward(HttpRequest request, HttpResponse response);
}
