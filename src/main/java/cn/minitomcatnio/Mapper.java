package cn.minitomcatnio;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 精确路径优先，其次最长前缀（/app/*）。匹配结果是 Wrapper。
 */
public class Mapper {

    private final Map<String, Wrapper> exactMappings = new LinkedHashMap<>();
    /** key 是前缀本身，例如 /app，对应模式 /app/* */
    private final Map<String, Wrapper> prefixMappings = new LinkedHashMap<>();

    public void addWrapper(String pattern, Wrapper wrapper) {
        if (pattern.endsWith("/*")) {
            prefixMappings.put(pattern.substring(0, pattern.length() - 2), wrapper);
            return;
        }
        exactMappings.put(pattern, wrapper);
    }

    public Match match(String path) {
        Wrapper exact = exactMappings.get(path);
        if (exact != null) {
            return new Match(exact, path, path, null);
        }

        String bestPrefix = null;
        Wrapper bestWrapper = null;
        for (Map.Entry<String, Wrapper> entry : prefixMappings.entrySet()) {
            String prefix = entry.getKey();
            if (!matchesPrefix(path, prefix)) {
                continue;
            }
            if (bestPrefix == null || prefix.length() > bestPrefix.length()) {
                bestPrefix = prefix;
                bestWrapper = entry.getValue();
            }
        }
        if (bestWrapper == null) {
            return null;
        }

        String pathInfo = path.length() == bestPrefix.length()
                ? null
                : path.substring(bestPrefix.length());
        return new Match(bestWrapper, bestPrefix + "/*", bestPrefix, pathInfo);
    }

    public Map<String, Wrapper> mappings() {
        Map<String, Wrapper> all = new LinkedHashMap<>(exactMappings);
        prefixMappings.forEach((prefix, wrapper) -> all.put(prefix + "/*", wrapper));
        return all;
    }

    private static boolean matchesPrefix(String path, String prefix) {
        if (prefix.isEmpty()) {
            return true;
        }
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    public static final class Match {
        public final Wrapper wrapper;
        public final String pattern;
        public final String servletPath;
        public final String pathInfo;

        Match(Wrapper wrapper, String pattern, String servletPath, String pathInfo) {
            this.wrapper = wrapper;
            this.pattern = pattern;
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
        }
    }
}
