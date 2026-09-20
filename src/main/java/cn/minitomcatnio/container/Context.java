package cn.minitomcatnio.container;

import cn.minitomcatnio.servlet.ApplicationFilterChain;
import cn.minitomcatnio.servlet.Filter;
import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;
import cn.minitomcatnio.servlet.Servlet;
import cn.minitomcatnio.session.SessionManager;
import cn.minitomcatnio.loader.StaticResourceProcessor;
import cn.minitomcatnio.loader.WebXmlLoader;
import cn.minitomcatnio.loader.WebappClassLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * ćĺ° Containerďźä¸ä¸Ş web ĺşç¨ăčŻˇćąĺčľ° Pipelineďźćĺä¸ĺłćĺĺ Servletă
 */
public class Context {

    private final Mapper mapper = new Mapper();
    private final SessionManager sessionManager = new SessionManager();
    private final Pipeline pipeline = new Pipeline();
    private final Path docBase;
    private final ClassLoader classLoader;
    private String path = "";
    private final List<FilterMapping> filterMappings = new ArrayList<>();

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

    public Mapper mapper() {
        return mapper;
    }

    public void invoke(HttpRequest request, HttpResponse response) {
        pipeline.invoke(request, response);
    }

    /**
     * web.xml čŁĺŽĺĺĺ§ĺćŻä¸Ş Servlet / Filterďźĺä¸ĺŽäžĺŞ init ä¸ćŹĄă
     */
    private void start() {
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
     * ĺşćŹéč°ç¨ďźĺ˝ä¸­ Wrapper ĺć§čĄďźĺŚĺčľ°éćčľćşă
     */
    void service(HttpRequest request, HttpResponse response) {
        request.setContextPath(path);
        request.bindContext(this);
        request.bindSession(sessionManager, response);
        dispatch(request, response);
    }

    /**
     * ĺ¨ĺ˝ĺ Context ĺĺĺăforward äšäźĺčľ°čżéă
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
