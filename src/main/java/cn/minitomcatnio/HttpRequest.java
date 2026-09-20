package cn.minitomcatnio;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次 HTTP 请求：请求行、Header、body、参数和 Cookie。
 */
public class HttpRequest {

    private final String method;
    private final String uri;
    private final String version;
    private final Map<String, String> headers;
    private final Map<String, String> parameters = new LinkedHashMap<>();
    private byte[] body = new byte[0];
    private String servletPath;
    private String pathInfo;
    private String contextPath = "";
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private SessionManager sessionManager;
    private HttpResponse response;
    private HttpSession session;
    private Context context;
    /** forward 后覆盖应用内路径；未 forward 时为 null。 */
    private String dispatchedPath;

    private HttpRequest(String method, String uri, String version, Map<String, String> headers) {
        this.method = method;
        this.uri = uri;
        this.version = version;
        this.headers = headers;
        parseQueryString();
        parseCookies();
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    /** 去掉 query string 后的路径，供 Servlet 映射使用。 */
    public String getPath() {
        int query = uri.indexOf('?');
        String path = query >= 0 ? uri.substring(0, query) : uri;
        return path.isEmpty() ? "/" : path;
    }

    public String getContextPath() {
        return contextPath;
    }

    /** 去掉 Context 路径后的剩余路径，供该应用内部映射使用。 */
    public String getPathWithinContext() {
        if (dispatchedPath != null) {
            return dispatchedPath;
        }
        String path = getPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return path;
        }
        if (path.equals(contextPath)) {
            return "/";
        }
        if (path.startsWith(contextPath + "/")) {
            String rest = path.substring(contextPath.length());
            return rest.isEmpty() ? "/" : rest;
        }
        return path;
    }

    void setContextPath(String contextPath) {
        this.contextPath = contextPath == null ? "" : contextPath;
    }

    void setDispatchedPath(String dispatchedPath) {
        this.dispatchedPath = dispatchedPath;
    }

    void bindContext(Context context) {
        this.context = context;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        if (context == null) {
            throw new IllegalStateException("Request is not bound to a Context");
        }
        return new ApplicationDispatcher(context, path);
    }

    public String getServletPath() {
        return servletPath;
    }

    /** 前缀映射多出来的路径，例如 /app/* 匹配 /app/user 时为 /user。 */
    public String getPathInfo() {
        return pathInfo;
    }

    void setMapping(String servletPath, String pathInfo) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
    }

    public String getVersion() {
        return version;
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    /**
     * HTTP/1.1 默认保活，HTTP/1.0 默认关闭；请求头 Connection 可以覆盖。
     */
    public boolean shouldKeepAlive() {
        String connection = getHeader("connection");
        if (version != null && version.toUpperCase().startsWith("HTTP/1.0")) {
            return connection != null && "keep-alive".equalsIgnoreCase(connection);
        }
        return connection == null || !"close".equalsIgnoreCase(connection);
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /** 同名参数只保留第一次出现的值。 */
    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getCookie(String name) {
        return cookies.get(name);
    }

    void bindSession(SessionManager sessionManager, HttpResponse response) {
        this.sessionManager = sessionManager;
        this.response = response;
    }

    /**
     * 按 Cookie 里的 JSESSIONID 取 Session；没有就新建并 Set-Cookie。
     */
    public HttpSession getSession() {
        if (session != null) {
            return session;
        }
        String requestedId = getCookie(SessionManager.COOKIE_NAME);
        session = sessionManager.getOrCreate(requestedId);
        if (requestedId == null || !requestedId.equals(session.getId())) {
            response.addCookie(SessionManager.COOKIE_NAME, session.getId());
        }
        return session;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }

    void setBody(byte[] body) {
        this.body = body == null ? new byte[0] : body;
        parseFormBody();
    }

    /**
     * 没有 Content-Length 时当作 0。非法值返回 -1。
     */
    public int getContentLength() {
        String value = getHeader("content-length");
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 在已读字节里找请求头结束位置（第一个 \\r 的下标）。
     * buffer 此时仍是写模式：position = 已读长度。
     */
    public static int indexOfHeaderEnd(ByteBuffer buffer) {
        byte[] arr = buffer.array();
        int length = buffer.position();
        for (int i = 0; i <= length - 4; i++) {
            if (arr[i] == '\r' && arr[i + 1] == '\n'
                    && arr[i + 2] == '\r' && arr[i + 3] == '\n') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 解析请求行和 Header。格式不对时返回 null。
     */
    public static HttpRequest parse(ByteBuffer buffer, int headerEnd) {
        byte[] arr = buffer.array();
        String headerBlock = new String(arr, 0, headerEnd, StandardCharsets.ISO_8859_1);
        String[] lines = headerBlock.split("\r\n");
        if (lines.length == 0) {
            return null;
        }

        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 3) {
            return null;
        }

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim().toLowerCase();
            String value = line.substring(colon + 1).trim();
            headers.put(name, value);
        }

        return new HttpRequest(requestLine[0], requestLine[1], requestLine[2], headers);
    }

    private void parseQueryString() {
        int query = uri.indexOf('?');
        if (query < 0 || query == uri.length() - 1) {
            return;
        }
        parseUrlEncoded(uri.substring(query + 1));
    }

    private void parseCookies() {
        String header = getHeader("cookie");
        if (header == null || header.isEmpty()) {
            return;
        }
        for (String part : header.split(";")) {
            String piece = part.trim();
            if (piece.isEmpty()) {
                continue;
            }
            int eq = piece.indexOf('=');
            String name = eq >= 0 ? piece.substring(0, eq).trim() : piece;
            String value = eq >= 0 ? piece.substring(eq + 1).trim() : "";
            cookies.putIfAbsent(name, value);
        }
    }

    private void parseFormBody() {
        String contentType = getHeader("content-type");
        if (contentType == null
                || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            return;
        }
        parseUrlEncoded(getBodyAsString());
    }

    private void parseUrlEncoded(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return;
        }
        for (String pair : encoded.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawName = eq >= 0 ? pair.substring(0, eq) : pair;
            String rawValue = eq >= 0 ? pair.substring(eq + 1) : "";
            parameters.putIfAbsent(decode(rawName), decode(rawValue));
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
