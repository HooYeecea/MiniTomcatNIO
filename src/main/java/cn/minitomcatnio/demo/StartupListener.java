package cn.minitomcatnio.demo;

import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.servlet.ServletContextListener;

/**
 * ROOT 应用的启动监听，用来验收 contextInitialized。
 */
public class StartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(Context context) {
        String path = context.getPath();
        System.out.println("Listener: contextInitialized path="
                + (path == null || path.isEmpty() ? "/" : path));
    }

    @Override
    public void contextDestroyed(Context context) {
        String path = context.getPath();
        System.out.println("Listener: contextDestroyed path="
                + (path == null || path.isEmpty() ? "/" : path));
    }
}
