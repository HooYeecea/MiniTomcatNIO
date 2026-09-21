package cn.minitomcatnio.container;

import cn.minitomcatnio.servlet.ApplicationDispatcher;
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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * ÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂ°ÄÂĂÂ ContainerÄÂĂÂĂĹĄÄšÂÄÂĂÂĂÂĂÂ¤ÄÂĂÂ¸ÄÂĂÂĂÂĂÂ¤ÄÂĂÂ¸ĂĹĄĂÂ web ÄÂÄšÂĂĹĄĂÂÄÂĂÂĂÂĂÂ§ÄÂĂÂÄÂĂÂ¨ÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂĂĹĄÄšÄ˝ÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšĹžÄÂĂÂ° PipelineÄÂĂÂĂĹĄÄšÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂĂÂĂÂ¤ÄÂĂÂ¸ÄÂĂÂÄÂÄšÂÄÂĂÂĂĹĄĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂ ServletÄÂĂÂÄÂĂÂÄÂĂÂ
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
    private final Map<Integer, String> errorPages = new HashMap<>();
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

    public void addErrorPage(int status, String location) {
        errorPages.put(status, location);
    }

    public Mapper mapper() {
        return mapper;
    }

    public void invoke(HttpRequest request, HttpResponse response) {
        pipeline.invoke(request, response);
    }

    /**
     * web.xml ÄÂĂÂĂĹĄĂÂÄÂĂÂÄÂÄšÂĂĹĄĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂ§ÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂĂÂĂĹĄÄšÄ˝ÄÂĂÂĂÂĂÂ¤ÄÂĂÂ¸ĂĹĄĂÂ Servlet / FilterÄÂĂÂĂĹĄÄšÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂĂÂĂÂ¤ÄÂĂÂ¸ÄÂĂÂÄÂÄšÂĂĹĄĂÂÄÂĂÂĂÂĂÂ¤ĂĹĄÄšĹžÄÂĂÂÄÂÄšÂÄÂĂÂĂĹĄĂÂ init ĂÂĂÂ¤ÄÂĂÂ¸ÄÂĂÂÄÂĂÂĂĹĄÄšÄÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ
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
     * ĺ destroy Servlet / FilterďźĺéçĽ listenerăč§ččŚćą listener çĺ°çćŻĺˇ˛éćŻççťäťśă
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
     * ÄÂÄšÂÄÂĂÂĂĹĄĂÂÄÂĂÂÄÂĂÂĂĹĄÄšÄĂÂÄšÂ ÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ°ÄÂĂÂĂÂĂÂ§ÄÂĂÂÄÂĂÂ¨ÄÂĂÂĂĹĄÄšÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂĂÂĂÂ¤ÄÂĂÂ¸ÄÂĂÂ­ Wrapper ÄÂÄšÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ§ÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂĂĹĄÄšÂÄÂĂÂÄÂÄšÂÄÂĂÂĂĹĄĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšĹžÄÂĂÂ°ĂÂÄšÂ ÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšĹžÄÂĂÂÄÂĂÂĂĹĄĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ
     */
    void service(HttpRequest request, HttpResponse response) {
        request.setContextPath(path);
        request.bindContext(this);
        request.bindSession(sessionManager, response);
        dispatch(request, response);
    }

    /**
     * ÄÂÄšÂÄÂĂÂÄÂĂÂ¨ÄÂÄšÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂ Context ÄÂÄšÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂforward ĂÂĂÂ¤ĂĹĄĂÂÄÂĂÂĂÂĂÂ¤ĂĹĄÄšÂÄÂĂÂÄÂÄšÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂÄšĹžÄÂĂÂ°ÄÂĂÂĂĹĄÄšĹÄÂĂÂĂÂÄšÂ ÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ
     */
    public void dispatch(HttpRequest request, HttpResponse response) {
        try {
            Mapper.Match match = mapper.match(request.getPathWithinContext());
            if (match != null) {
                request.setMapping(match.servletPath, match.pathInfo);
                List<Filter> filters = matchingFilters(request.getPathWithinContext());
                new ApplicationFilterChain(filters, match.wrapper).doFilter(request, response);
                return;
            }
            StaticResourceProcessor.process(request, response, docBase);
        } catch (Exception e) {
            if (!sendErrorPage(request, response, e)) {
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException(e);
            }
        }
    }

    /** 按 web.xml 的 error-page 转发。forward 会把状态重置成 200，结束后再写回 500。 */
    private boolean sendErrorPage(HttpRequest request, HttpResponse response, Exception error) {
        if (request.isErrorDispatch()) {
            return false;
        }
        String location = errorPages.get(500);
        if (location == null) {
            return false;
        }
        System.out.println("error-page 500 -> " + location + ": " + error);
        request.setErrorDispatch(true);
        new ApplicationDispatcher(this, location).forward(request, response);
        response.setStatus(500, "Internal Server Error");
        return true;
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
