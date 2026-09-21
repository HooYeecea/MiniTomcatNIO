package cn.minitomcatnio.container;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 匹配顺序：精确 → 最长前缀（/app/*）→ 扩展名（*.do）→ 默认 Servlet（/）。
 */
public class Mapper {

    private final Map<String, Wrapper> exactMappings = new LinkedHashMap<>();
    /** key 是前缀本身，例如 /app，对应模式 /app/* */
    private final Map<String, Wrapper> prefixMappings = new LinkedHashMap<>();
    /** key 是扩展名，例如 do，对应模式 *.do */
    private final Map<String, Wrapper> extensionMappings = new LinkedHashMap<>();
    /** url-pattern 为 / 的默认 Servlet，优先级最低。 */
    private Wrapper defaultWrapper;

    public void addWrapper(String pattern, Wrapper wrapper) {
        if ("/".equals(pattern)) {
            defaultWrapper = wrapper;
            return;
        }
        if (pattern.startsWith("*.") && pattern.length() > 2 && pattern.indexOf('/') < 0) {
            extensionMappings.put(pattern.substring(2), wrapper);
            return;
        }
        if (pattern.endsWith("/*")) {
            prefixMappings.put(pattern.substring(0, pattern.length() - 2), wrapper);
            return;
        }
        exactMappings.put(pattern, wrapper);
    }

    public boolean hasDefault() {
        return defaultWrapper != null;
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
        if (bestWrapper != null) {
            String pathInfo = path.length() == bestPrefix.length()
                    ? null
                    : path.substring(bestPrefix.length());
            return new Match(bestWrapper, bestPrefix + "/*", bestPrefix, pathInfo);
        }

        String extension = extensionOf(path);
        if (extension != null) {
            Wrapper extensionWrapper = extensionMappings.get(extension);
            if (extensionWrapper != null) {
                return new Match(extensionWrapper, "*." + extension, path, null);
            }
        }
        if (defaultWrapper != null) {
            return new Match(defaultWrapper, "/", path, null);
        }
        return null;
    }

    public Map<String, Wrapper> mappings() {
        Map<String, Wrapper> all = new LinkedHashMap<>(exactMappings);
        prefixMappings.forEach((prefix, wrapper) -> all.put(prefix + "/*", wrapper));
        extensionMappings.forEach((ext, wrapper) -> all.put("*." + ext, wrapper));
        if (defaultWrapper != null) {
            all.put("/", defaultWrapper);
        }
        return all;
    }

    private static boolean matchesPrefix(String path, String prefix) {
        if (prefix.isEmpty()) {
            return true;
        }
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    /** 取路径最后一段里的扩展名；没有点或点在最后一段开头则返回 null。 */
    private static String extensionOf(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot <= slash || dot == path.length() - 1) {
            return null;
        }
        return path.substring(dot + 1);
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
