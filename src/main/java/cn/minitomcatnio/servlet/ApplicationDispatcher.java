package cn.minitomcatnio.servlet;

import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

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
        String target = normalize(path);
        response.reset();
        request.setDispatchedPath(target);
        context.dispatch(request, response);
    }

    @Override
    public void include(HttpRequest request, HttpResponse response) {
        String previous = request.getDispatchedPath();
        request.setDispatchedPath(normalize(path));
        HttpResponse included = new HttpResponse();
        context.dispatch(request, included);
        request.setDispatchedPath(previous);
        response.appendBody(included.getBody());
    }

    private static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
