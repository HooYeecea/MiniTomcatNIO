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
 * ÄÂÂÄşÂ°Â ContainerÄĹşÂĂ¤Â¸ÂĂ¤Â¸Ĺ web ÄşĹÂĂ§ÂÂ¨ÄÂÂÄĹťËÄÄÂÄşÂÂÄÄžÂ° PipelineÄĹşÂÄÂÂÄşÂÂĂ¤Â¸ÂÄşÂĹÄÂÂÄşÂÂÄşÂÂ ServletÄÂÂ
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
     * web.xml ÄĹÂÄşĹ˝ÂÄşÂÂÄşÂÂÄşÂ§ÂÄşÂÂÄĹťÂĂ¤Â¸Ĺ Servlet / FilterÄĹşÂÄşÂÂĂ¤Â¸ÂÄşĹ˝ÂĂ¤ĹžÂÄşÂĹ init Ă¤Â¸ÂÄĹšÄÄÂÂ
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
     * ÄşÂĹÄÂĹšĂŠÂÂÄÂ°ÂĂ§ÂÂ¨ÄĹşÂÄşÂËĂ¤Â¸Â­ Wrapper ÄşÂÂÄÂÂ§ÄÄÂÄĹşÂÄşÂĹÄşÂÂÄÄžÂ°ĂŠÂÂÄÂÂÄÄžÂÄĹÂÄÂÂ
     */
    void service(HttpRequest request, HttpResponse response) {
        request.setContextPath(path);
        request.bindContext(this);
        request.bindSession(sessionManager, response);
        dispatch(request, response);
    }

    /**
     * ÄşÂÂ¨ÄşËÂÄşÂÂ Context ÄşÂÂÄşÂÂÄşÂÂÄÂÂforward Ă¤ĹĄÂĂ¤ĹşÂÄşÂÂÄÄžÂ°ÄĹźÂĂŠÂÂÄÂÂ
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
