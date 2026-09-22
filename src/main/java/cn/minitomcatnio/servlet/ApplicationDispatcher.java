package cn.minitomcatnio.servlet;

import cn.minitomcatnio.container.Context;
import com.web.DispatcherType;
import com.web.HttpRequest;
import com.web.HttpResponse;
import com.web.RequestDispatcher;

/**
 * 同应用内的 forward / include。
 */
public class ApplicationDispatcher implements RequestDispatcher {

    private final Context context;
    private final String path;

    public ApplicationDispatcher(Context context, String path) {
        this.context = context;
        this.path = path;
    }

    @Override
    public void forward(HttpRequest request, HttpResponse response) {
        cn.minitomcatnio.http.HttpRequest req = (cn.minitomcatnio.http.HttpRequest) request;
        cn.minitomcatnio.http.HttpResponse resp = (cn.minitomcatnio.http.HttpResponse) response;
        String target = normalize(path);
        resp.reset();
        req.setDispatchedPath(target);
        // error-page 已经标成 ERROR，不要被 forward 盖成 FORWARD
        if (req.getDispatcherType() != DispatcherType.ERROR) {
            req.setDispatcherType(DispatcherType.FORWARD);
        }
        context.dispatch(req, resp);
    }

    @Override
    public void include(HttpRequest request, HttpResponse response) {
        cn.minitomcatnio.http.HttpRequest req = (cn.minitomcatnio.http.HttpRequest) request;
        cn.minitomcatnio.http.HttpResponse resp = (cn.minitomcatnio.http.HttpResponse) response;
        String previousPath = req.getDispatchedPath();
        DispatcherType previousType = req.getDispatcherType();
        req.setDispatchedPath(normalize(path));
        req.setDispatcherType(DispatcherType.INCLUDE);
        cn.minitomcatnio.http.HttpResponse included = new cn.minitomcatnio.http.HttpResponse();
        context.dispatch(req, included);
        req.setDispatchedPath(previousPath);
        req.setDispatcherType(previousType);
        resp.appendBody(included.getBody());
    }

    private static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
