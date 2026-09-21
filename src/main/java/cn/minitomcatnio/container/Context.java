package cn.minitomcatnio.container;

import cn.minitomcatnio.servlet.ApplicationFilterChain;
import cn.minitomcatnio.servlet.Filter;
import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;
import cn.minitomcatnio.servlet.ServletContextListener;
import cn.minitomcatnio.session.SessionManager;
import cn.minitomcatnio.loader.StaticResourceProcessor;
import cn.minitomcatnio.loader.WebXmlLoader;
import cn.minitomcatnio.loader.WebappClassLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * ĂÂĂÂĂÂĂĹĂÂ°ĂÂ ContainerĂÂÄšĹĂÂÄÂ¤ĂÂ¸ĂÂÄÂ¤ĂÂ¸ÄšÂ web ĂĹÄšÂĂÂÄÂ§ĂÂĂÂ¨ĂÂĂÂĂÂĂÂÄšĹĽĂÂĂÂĂÂĂÂĂĹĂÂĂÂĂÂĂĹžĂÂ° PipelineĂÂÄšĹĂÂĂÂĂÂĂÂĂĹĂÂĂÂÄÂ¤ĂÂ¸ĂÂĂĹĂÂÄšÂĂÂĂÂĂÂĂĹĂÂĂÂĂĹĂÂĂÂ ServletĂÂĂÂĂÂ
 */
public class Context {

    private final Mapper mapper = new Mapper();
    private final SessionManager sessionManager = new SessionManager();
    private final Pipeline pipeline = new Pipeline();
    private final Path docBase;
    private final ClassLoader classLoader;
    private String path = "";
    private final List<FilterMapping> filterMappings = new ArrayList<>();
    private final List<ServletContextListener> listeners = new ArrayList<>();
    private boolean stopped;

    public Context(Path docBase) {
        this.docBase = docBase.toAbsolutePath().normalize();
        this.classLoader = new WebappClassLoader(this.docBase, getClass().getClassLoader());
        pipeline.addValve(new AccessLogValve());
        pipeline.setBasic(new StandardContextValve(this));
        WebXmlLoader.load(this);
        start();
    }

    public Path getDocBase() {
        return docBase;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path == null ? "" : path;
    }

    public void addServlet(String pattern, Servlet servlet) {
        mapper.addWrapper(pattern, new Wrapper(pattern, servlet));
    }

    public void addFilter(String pattern, Filter filter) {
        filterMappings.add(new FilterMapping(pattern, filter));
    }

    public void addListener(ServletContextListener listener) {
        listeners.add(listener);
    }

    public Mapper mapper() {
        return mapper;
    }

    public void invoke(HttpRequest request, HttpResponse response) {
        pipeline.invoke(request, response);
    }

    /**
     * web.xml ĂÂÄšÂĂÂĂĹÄšËĂÂĂĹĂÂĂÂĂĹĂÂĂÂĂĹĂÂ§ĂÂĂĹĂÂĂÂĂÂÄšĹĽĂÂÄÂ¤ĂÂ¸ÄšÂ Servlet / FilterĂÂÄšĹĂÂĂĹĂÂĂÂÄÂ¤ĂÂ¸ĂÂĂĹÄšËĂÂÄÂ¤ÄšĹžĂÂĂĹĂÂÄšÂ init ÄÂ¤ĂÂ¸ĂÂĂÂÄšĹĄĂÂĂÂĂÂĂÂ
     */
    private void start() {
        for (ServletContextListener listener : listeners) {
            listener.contextInitialized(this);
        }
        IdentityHashMap<Servlet, Boolean> startedServlets = new IdentityHashMap<>();
        for (Wrapper wrapper : mapper.mappings().values()) {
            Servlet servlet = wrapper.getServlet();
            if (startedServlets.put(servlet, Boolean.TRUE) == null) {
                servlet.init();
            }
        }
        IdentityHashMap<Filter, Boolean> startedFilters = new IdentityHashMap<>();
        for (FilterMapping mapping : filterMappings) {
            if (startedFilters.put(mapping.filter, Boolean.TRUE) == null) {
                mapping.filter.init();
            }
        }
    }

    /**
     * 先 destroy Servlet / Filter，再通知 listener。规范要求 listener 看到的是已销毁的组件。
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;

        IdentityHashMap<Servlet, Boolean> stoppedServlets = new IdentityHashMap<>();
        for (Wrapper wrapper : mapper.mappings().values()) {
            Servlet servlet = wrapper.getServlet();
            if (stoppedServlets.put(servlet, Boolean.TRUE) == null) {
                servlet.destroy();
            }
        }
        IdentityHashMap<Filter, Boolean> stoppedFilters = new IdentityHashMap<>();
        for (FilterMapping mapping : filterMappings) {
            if (stoppedFilters.put(mapping.filter, Boolean.TRUE) == null) {
                mapping.filter.destroy();
            }
        }
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).contextDestroyed(this);
        }
    }

    /**
     * ĂĹĂÂÄšÂĂÂĂÂÄšĹĄÄĹ ĂÂĂÂĂÂĂÂ°ĂÂÄÂ§ĂÂĂÂ¨ĂÂÄšĹĂÂĂĹĂÂĂÂÄÂ¤ĂÂ¸ĂÂ­ Wrapper ĂĹĂÂĂÂĂÂĂÂĂÂ§ĂÂĂÂĂÂĂÂÄšĹĂÂĂĹĂÂÄšÂĂĹĂÂĂÂĂÂĂĹžĂÂ°ÄĹ ĂÂĂÂĂÂĂÂĂÂĂÂĂĹžĂÂĂÂÄšÂĂÂĂÂĂÂĂÂ
     */
    void service(HttpRequest request, HttpResponse response) {
        request.setContextPath(path);
        request.bindContext(this);
        request.bindSession(sessionManager, response);
        dispatch(request, response);
    }

    /**
     * ĂĹĂÂĂÂ¨ĂĹĂÂĂÂĂĹĂÂĂÂ Context ĂĹĂÂĂÂĂĹĂÂĂÂĂĹĂÂĂÂĂÂĂÂĂÂforward ÄÂ¤ÄšÄĂÂÄÂ¤ÄšĹĂÂĂĹĂÂĂÂĂÂĂĹžĂÂ°ĂÂÄšĹşĂÂÄĹ ĂÂĂÂĂÂĂÂĂÂ
     */
    public void dispatch(HttpRequest request, HttpResponse response) {
        Mapper.Match match = mapper.match(request.getPathWithinContext());
        if (match != null) {
            request.setMapping(match.servletPath, match.pathInfo);
            List<Filter> filters = matchingFilters(request.getPathWithinContext());
            new ApplicationFilterChain(filters, match.wrapper).doFilter(request, response);
            return;
        }
        StaticResourceProcessor.process(request, response, docBase);
    }

    private List<Filter> matchingFilters(String pathWithinContext) {
        List<Filter> matched = new ArrayList<>();
        for (FilterMapping mapping : filterMappings) {
            if (matchesFilter(pathWithinContext, mapping.pattern)) {
                matched.add(mapping.filter);
            }
        }
        return matched;
    }

    private static boolean matchesFilter(String path, String pattern) {
        if ("/*".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return path.equals(pattern);
    }

    private static final class FilterMapping {
        final String pattern;
        final Filter filter;

        FilterMapping(String pattern, Filter filter) {
            this.pattern = pattern;
            this.filter = filter;
        }
    }
}
