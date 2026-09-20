package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Host：一个虚拟主机。下面挂若干 Context，按 URI 前缀选择应用。
 */
public class Host {

    private final String name;
    private final Map<String, Context> contexts = new LinkedHashMap<>();
    private final Pipeline pipeline = new Pipeline();

    public Host(String name) {
        this.name = name;
        pipeline.setBasic(new StandardHostValve(this));
    }

    public String getName() {
        return name;
    }

    public void addContext(String path, Context context) {
        String normalized = normalize(path);
        context.setPath(normalized);
        contexts.put(normalized, context);
    }

    public Map<String, Context> contexts() {
        return contexts;
    }

    public void invoke(HttpRequest request, HttpResponse response) {
        pipeline.invoke(request, response);
    }

    Context findContext(String uriPath) {
        Context best = contexts.get("");
        int bestLen = best == null ? -1 : 0;
        for (Map.Entry<String, Context> entry : contexts.entrySet()) {
            String contextPath = entry.getKey();
            if (contextPath.isEmpty()) {
                continue;
            }
            if ((uriPath.equals(contextPath) || uriPath.startsWith(contextPath + "/"))
                    && contextPath.length() > bestLen) {
                best = entry.getValue();
                bestLen = contextPath.length();
            }
        }
        return best;
    }

    private static String normalize(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}
