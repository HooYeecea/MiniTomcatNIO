package cn.minitomcatnio.servlet;

import com.web.Servlet;
import com.web.ServletConfig;

/**
 * 记住容器传入的 ServletConfig，这样 service 里也能读 init-param。
 */
public abstract class GenericServlet implements Servlet {

    private ServletConfig config;

    @Override
    public void init(ServletConfig config) {
        this.config = config;
        init();
    }

    @Override
    public String getInitParameter(String name) {
        return config == null ? null : config.getInitParameter(name);
    }
}
