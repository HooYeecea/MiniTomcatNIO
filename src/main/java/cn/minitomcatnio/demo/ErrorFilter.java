package cn.minitomcatnio.demo;

import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.Filter;
import com.web.FilterChain;

/**
 * 只在 ERROR 派发时执行，用来验收 filter-mapping 的 dispatcher。
 */
public class ErrorFilter implements Filter {

    @Override
    public void init() {
        System.out.println("Filter init: ErrorFilter");
    }

    @Override
    public void destroy() {
        System.out.println("Filter destroy: ErrorFilter");
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) throws Exception {
        System.out.println("ErrorFilter: " + request.getPath()
                + " dispatcher=" + request.getDispatcherType());
        chain.doFilter(request, response);
    }
}
