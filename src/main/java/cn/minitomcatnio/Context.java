package cn.minitomcatnio;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 最小 Container：一个 web 应用。请求先走 Pipeline，最后一关才分发 Servlet。
 */
public class Context {

    private final Mapper mapper = new Mapper();
    private final SessionManager sessionManager = new SessionManager();
    private final Pipeline pipeline = new Pipeline();
    private final Path docBase;
    private String path = "";
    private final List<FilterMapping> filterMappings = new ArrayList<>();

    public Context(Path docBase) {
        this.docBase = docBase.toAbsolutePath().normalize();
        pipeline.addValve(new AccessLogValve());
        pipeline.setBasic(new StandardContextValve(this));
        WebXmlLoader.load(this);
    }

    public Path getDocBase() {
        return docBase;
    }

    public String getPath() {
        return path;
    }

    void setPath(String path) {
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
     * 基本阀调用：命中 Wrapper 则执行，否则走静态资源。
     */
    void service(HttpRequest request, HttpResponse response) {
        request.setContextPath(path);
        Mapper.Match match = mapper.match(request.getPathWithinContext());
        if (match != null) {
            request.setMapping(match.servletPath, match.pathInfo);
            request.bindSession(sessionManager, response);
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
