package cn.minitomcatnio;

/**
 * 最小 Container：一个 web 应用。负责 Servlet 映射、Session 和静态资源。
 */
public class Context {

    private final Mapper mapper = new Mapper();
    private final SessionManager sessionManager = new SessionManager();

    public void addServlet(String pattern, Servlet servlet) {
        mapper.addServlet(pattern, servlet);
    }

    public Mapper mapper() {
        return mapper;
    }

    public void invoke(HttpRequest request, HttpResponse response) {
        Mapper.Match match = mapper.match(request.getPath());
        if (match != null) {
            request.setMapping(match.servletPath, match.pathInfo);
            request.bindSession(sessionManager, response);
            match.servlet.service(request, response);
            return;
        }
        StaticResourceProcessor.process(request, response);
    }
}
