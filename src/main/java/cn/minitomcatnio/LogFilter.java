package cn.minitomcatnio;

/**
 * ROOT 应用的示例 Filter，用来验收过滤器链。
 */
public class LogFilter implements Filter {

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        System.out.println("Filter: " + request.getPath());
        chain.doFilter(request, response);
    }
}
