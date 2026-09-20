package cn.minitomcatnio.demo;

import cn.minitomcatnio.servlet.Filter;
import cn.minitomcatnio.servlet.FilterChain;
import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * ROOT 应用的示例 Filter，用来验收过滤器链。
 */
public class LogFilter implements Filter {

    @Override
    public void init() {
        System.out.println("Filter init: LogFilter");
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        System.out.println("Filter: " + request.getPath());
        chain.doFilter(request, response);
    }
}
