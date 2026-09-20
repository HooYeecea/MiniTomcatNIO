package cn.minitomcatnio;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 精确路径匹配。命中则交给 Servlet，未命中由调用方走静态资源。
 */
public class Mapper {

    private final Map<String, Servlet> exactMappings = new LinkedHashMap<>();

    public void addServlet(String path, Servlet servlet) {
        exactMappings.put(path, servlet);
    }

    public Servlet match(String path) {
        return exactMappings.get(path);
    }

    public Map<String, Servlet> mappings() {
        return exactMappings;
    }
}
