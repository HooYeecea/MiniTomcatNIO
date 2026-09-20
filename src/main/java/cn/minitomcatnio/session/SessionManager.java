package cn.minitomcatnio.session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 id 保存 Session。找不到或没有 JSESSIONID 时新建。
 */
public class SessionManager {

    public static final String COOKIE_NAME = "JSESSIONID";

    private final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<>();

    public HttpSession getOrCreate(String id) {
        if (id != null && !id.isEmpty()) {
            HttpSession existing = sessions.get(id);
            if (existing != null) {
                return existing;
            }
        }
        HttpSession created = new HttpSession(newId());
        sessions.put(created.getId(), created);
        return created;
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
