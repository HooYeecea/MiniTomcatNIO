package cn.minitomcatnio.servlet;

import cn.minitomcatnio.container.Context;

/**
 * 应用生命周期回调。contextInitialized 在 Servlet init 之前；
 * contextDestroyed 在 Servlet / Filter destroy 之后。
 */
public interface ServletContextListener {

    void contextInitialized(Context context);

    default void contextDestroyed(Context context) {
    }
}
