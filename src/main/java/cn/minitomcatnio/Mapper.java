package cn.minitomcatnio;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 精确路径优先，其次最长前缀（/app/*）。未命中由调用方走静态资源。
 */
public class Mapper {

    private final Map<String, Servlet> exactMappings = new LinkedHashMap<>();
    /** key 是前缀本身，例如 /app，对应模式 /app/* */
    private final Map<String, Servlet> prefixMappings = new LinkedHashMap<>();

    public void addServlet(String pattern, Servlet servlet) {
        if (pattern.endsWith("/*")) {
            prefixMappings.put(pattern.substring(0, pattern.length() - 2), servlet);
            return;
        }
        exactMappings.put(pattern, servlet);
    }

    public Match match(String path) {
        Servlet exact = exactMappings.get(path);
        if (exact != null) {
            return new Match(exact, path, path, null);
        }

        String bestPrefix = null;
        Servlet bestServlet = null;
        for (Map.Entry<String, Servlet> entry : prefixMappings.entrySet()) {
            String prefix = entry.getKey();
            if (!matchesPrefix(path, prefix)) {
                continue;
            }
            if (bestPrefix == null || prefix.length() > bestPrefix.length()) {
                bestPrefix = prefix;
                bestServlet = entry.getValue();
            }
        }
        if (bestServlet == null) {
            return null;
        }

        String pathInfo = path.length() == bestPrefix.length()
                ? null
                : path.substring(bestPrefix.length());
        return new Match(bestServlet, bestPrefix + "/*", bestPrefix, pathInfo);
    }

    public Map<String, Servlet> mappings() {
        Map<String, Servlet> all = new LinkedHashMap<>(exactMappings);
        prefixMappings.forEach((prefix, servlet) -> all.put(prefix + "/*", servlet));
        return all;
    }

    private static boolean matchesPrefix(String path, String prefix) {
        if (prefix.isEmpty()) {
            return true;
        }
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    public static final class Match {
        public final Servlet servlet;
        public final String pattern;
        public final String servletPath;
        public final String pathInfo;

        Match(Servlet servlet, String pattern, String servletPath, String pathInfo) {
            this.servlet = servlet;
            this.pattern = pattern;
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
        }
    }
}
