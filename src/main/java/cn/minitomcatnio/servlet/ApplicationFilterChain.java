package cn.minitomcatnio.servlet;

import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.container.Wrapper;
import com.web.Filter;
import com.web.FilterChain;
import com.web.HttpRequest;
import com.web.HttpResponse;

import java.util.List;

/**
 * 先按顺序执行匹配到的 Filter，最后一关调用 Wrapper 里的 Servlet。
 */
public class ApplicationFilterChain implements FilterChain {

    private final List<Filter> filters;
    private final Wrapper wrapper;
    private int index;

    public ApplicationFilterChain(List<Filter> filters, Wrapper wrapper) {
        this.filters = filters;
        this.wrapper = wrapper;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response) throws Exception {
        if (index < filters.size()) {
            filters.get(index++).doFilter(request, response, this);
            return;
        }
        wrapper.invoke(
                (cn.minitomcatnio.http.HttpRequest) request,
                (cn.minitomcatnio.http.HttpResponse) response);
    }
}
