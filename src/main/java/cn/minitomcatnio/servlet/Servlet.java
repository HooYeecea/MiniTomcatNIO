package cn.minitomcatnio.servlet;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * 最小 Servlet：一次请求进来，自己往 HttpResponse 里写。
 */
public interface Servlet {

    default void init() {
    }

    void service(HttpRequest request, HttpResponse response);
}
