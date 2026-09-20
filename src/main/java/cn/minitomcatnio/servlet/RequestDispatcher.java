package cn.minitomcatnio.servlet;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * Servlet 规范里的 RequestDispatcher。这一步只实现 forward。
 */
public interface RequestDispatcher {

    void forward(HttpRequest request, HttpResponse response);
}
