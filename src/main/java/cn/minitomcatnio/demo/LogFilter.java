package cn.minitomcatnio.demo;

import com.minispring.web.Filter;
import com.minispring.web.FilterChain;
import com.minispring.web.HttpRequest;
import com.minispring.web.HttpResponse;

/**
 * ROOT 应用的示例 Filter，用来验收过滤器链。
 */
public class LogFilter implements Filter {

    @Override
    public void init() {
        System.out.println("Filter init: LogFilter");
    }

    @Override
    public void destroy() {
        System.out.println("Filter destroy: LogFilter");
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws Exception {
        System.out.println("Filter: " + request.getPath()
                + " dispatcher=" + request.getDispatcherType());
        chain.doFilter(request, response);
    }
}
