package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * 容器关卡。处理完自己的事后，再调用 chain 把请求交给下一关。
 */
public interface Valve {

    void invoke(HttpRequest request, HttpResponse response, ValveChain chain);
}
