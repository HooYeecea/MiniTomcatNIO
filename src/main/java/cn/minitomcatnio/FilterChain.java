package cn.minitomcatnio;

/**
 * Filter 链的剩余部分。
 */
public interface FilterChain {

    void doFilter(HttpRequest request, HttpResponse response);
}
