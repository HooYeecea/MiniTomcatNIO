package cn.minitomcatnio.servlet;

import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

/**
 * 把当前请求转到同应用的另一个路径（只做 forward，先不做 include）。
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

    private static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
