package cn.minitomcatnio.servlet;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * 应用过滤器。处理完后必须调用 chain，才会走到下一个 Filter 或 Servlet。
 */
public interface Filter {

    default void init() {
    }

    default void destroy() {
    }

    void doFilter(HttpRequest request, HttpResponse response, FilterChain chain);
}
