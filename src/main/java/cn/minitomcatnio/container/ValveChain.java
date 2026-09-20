package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * Valve 管道的剩余部分。
 */
public interface ValveChain {

    void invoke(HttpRequest request, HttpResponse response);
}
