package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * 容器访问日志。先放行下一关，回来再打方法、路径、状态码。
 */
public class AccessLogValve implements Valve {

    @Override
    public void invoke(HttpRequest request, HttpResponse response, ValveChain chain) {
        chain.invoke(request, response);
        System.out.println("AccessLog: " + request.getMethod() + " "
                + request.getPath() + " " + response.getStatus());
    }
}
