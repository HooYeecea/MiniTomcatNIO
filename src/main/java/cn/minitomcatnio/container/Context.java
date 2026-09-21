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
 * ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂ°ĂÂĂÂÄÂĂÂ ContainerĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂÂĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ĂÂĂÂÄÂĂÂ¸ĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ĂÂĂÂÄÂĂÂ¸ÄÂÄšÄÄÂĂÂ web ĂÂĂÂĂĹĄĂÂÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ§ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ¨ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂËĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄÄšĹžĂÂĂÂÄÂĂÂ° PipelineĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ĂÂĂÂÄÂĂÂ¸ĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ ServletĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ
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
     * web.xml ĂÂĂÂÄÂĂÂÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂ§ĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂËĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ĂÂĂÂÄÂĂÂ¸ÄÂÄšÄÄÂĂÂ Servlet / FilterĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ĂÂĂÂÄÂĂÂ¸ĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ÄÂÄšÄĂĹĄÄšĹžĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂÄÂÄšÄÄÂĂÂ init ÄÂĂÂÄÂĂÂ¤ĂÂĂÂÄÂĂÂ¸ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ
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
     * ÄşÂÂ destroy Servlet / FilterÄĹşÂÄşÂÂĂŠÂÂĂ§ÂÄ˝ listenerÄÂÂÄÂ§ÂÄÂÂÄĹÂÄÄÂ listener Ă§ÂÂÄşÂÂ°Ă§ÂÂÄÂĹťÄşËËĂŠÂÂÄĹťÂĂ§ÂÂĂ§ĹĽÂĂ¤ĹĽĹÄÂÂ
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
     * ĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂÂÄÂĂÂĂĹĄĂÂ ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ°ĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ§ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ¨ĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ĂÂĂÂÄÂĂÂ¸ĂÂĂÂÄÂĂÂ­ Wrapper ĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ§ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂÄÂÄšÄÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄÄšĹžĂÂĂÂÄÂĂÂ°ÄÂĂÂĂĹĄĂÂ ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄÄšĹžĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ
     */
    void service(HttpRequest request, HttpResponse response) {
        request.setContextPath(path);
        request.bindContext(this);
        request.bindSession(sessionManager, response);
        dispatch(request, response);
    }

    /**
     * ĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ¨ĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ Context ĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂforward ÄÂĂÂÄÂĂÂ¤ÄÂÄšÄÄÂĂÂĂÂĂÂÄÂĂÂÄÂĂÂÄÂĂÂ¤ÄÂÄšÄĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂĂĹĄÄšĹžĂÂĂÂÄÂĂÂ°ĂÂĂÂÄÂĂÂÄÂÄšÄĂĹĄÄšÂĂÂĂÂÄÂĂÂÄÂĂÂĂĹĄĂÂ ĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂĂÂĂÂÄÂĂÂ
     */
    public void dispatch(HttpRequest request, HttpResponse response) {
        try {
            Mapper.Match match = mapper.match(request.getPathWithinContext());
            if (match != null) {
                request.setMapping(match.servletPath, match.pathInfo);
                List<Filter> filters = matchingFilters(request.getPathWithinContext());
                new ApplicationFilterChain(filters, match.wrapper).doFilter(request, response);
            } else {
                StaticResourceProcessor.process(request, response, docBase);
            }
        } catch (Exception e) {
            if (!sendErrorPage(request, response, 500, "Internal Server Error", e)) {
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException(e);
            }
            return;
        }
        if (response.isError()) {
            sendErrorPage(request, response, response.getStatus(), response.getReason(), null);
        }
    }

    /** ć web.xml ç error-page č˝Źĺăforward äźćçśćéç˝Žć 200ďźçťćĺĺĺĺ 500ă */
    private boolean sendErrorPage(HttpRequest request, HttpResponse response, int status, String reason, Exception error) {
        if (request.isErrorDispatch()) {
            return false;
        }
        String location = errorPages.get(status);
        if (location == null) {
            return false;
        }
        System.out.println("error-page " + status + " -> " + location
                + (error == null ? "" : ": " + error));
        request.setErrorDispatch(true);
        new ApplicationDispatcher(this, location).forward(request, response);
        response.setStatus(status, reason);
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
