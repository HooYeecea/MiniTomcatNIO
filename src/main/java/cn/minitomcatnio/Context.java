package cn.minitomcatnio;

import java.nio.file.Path;

/**
 * 最小 Container：一个 web 应用。请求先走 Pipeline，最后一关才分发 Servlet。
 */
public class Context {

    private final Mapper mapper = new Mapper();
    private final SessionManager sessionManager = new SessionManager();
    private final Pipeline pipeline = new Pipeline();
    private final Path docBase;
    private String path = "";

    public Context(Path docBase) {
        this.docBase = docBase.toAbsolutePath().normalize();
        pipeline.addValve(new AccessLogValve());
        pipeline.setBasic(new StandardContextValve(this));
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
            match.wrapper.invoke(request, response);
            return;
        }
        StaticResourceProcessor.process(request, response, docBase);
    }
}
