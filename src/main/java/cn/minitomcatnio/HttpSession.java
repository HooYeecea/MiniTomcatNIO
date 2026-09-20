package cn.minitomcatnio;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端会话。这一步只做属性存取，先不过期。
 */
public class HttpSession {

    private final String id;
    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    HttpSession(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
            return;
        }
        attributes.put(name, value);
    }
}
