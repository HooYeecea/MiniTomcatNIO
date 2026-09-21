package cn.minitomcatnio.servlet;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * 最小 Servlet：一次请求进来，自己往 HttpResponse 里写。
 */
public interface Servlet {

    default void init() {
    }

    /** init 之前由容器注入。默认实现丢掉配置，需要读参数的 Servlet 覆盖它。 */
    default void init(ServletConfig config) {
        init();
    }

    default String getInitParameter(String name) {
        return null;
    }

    default void destroy() {
    }

    void service(HttpRequest request, HttpResponse response);
}
