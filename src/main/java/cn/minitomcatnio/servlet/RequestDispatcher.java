package cn.minitomcatnio.servlet;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * Servlet 规范里的 RequestDispatcher。支持 forward 和 include。
 */
public interface RequestDispatcher {

    void forward(HttpRequest request, HttpResponse response);

    void include(HttpRequest request, HttpResponse response);
}
