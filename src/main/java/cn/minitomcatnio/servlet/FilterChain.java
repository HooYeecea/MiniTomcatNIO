package cn.minitomcatnio.servlet;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * Filter 链的剩余部分。
 */
public interface FilterChain {

    void doFilter(HttpRequest request, HttpResponse response);
}
