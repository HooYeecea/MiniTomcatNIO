package cn.minitomcatnio;

/**
 * Valve 管道的剩余部分。
 */
public interface ValveChain {

    void invoke(HttpRequest request, HttpResponse response);
}
