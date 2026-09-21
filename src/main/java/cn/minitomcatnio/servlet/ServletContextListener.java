package cn.minitomcatnio.servlet;

import cn.minitomcatnio.container.Context;

/**
 * 应用启动回调。Context start 时、Servlet init 之前调用。
 */
public interface ServletContextListener {

    void contextInitialized(Context context);
}
