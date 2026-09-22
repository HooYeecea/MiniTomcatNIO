package cn.minitomcatnio.session;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端会话。这一步只做属性存取，先不过期。
 */
public class HttpSession implements com.web.HttpSession {

    private final String id;
    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    HttpSession(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
            return;
        }
        attributes.put(name, value);
    }
}
