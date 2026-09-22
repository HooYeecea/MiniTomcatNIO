package cn.minitomcatnio.http;

import cn.minitomcatnio.servlet.ApplicationDispatcher;
import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.session.SessionManager;
import com.web.DispatcherType;
import com.web.RequestDispatcher;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NIO connector request: method, URI, headers, body, cookies, session binding.
 */
public class HttpRequest implements com.web.HttpRequest {

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
    private cn.minitomcatnio.session.HttpSession session;
    private Context context;
    /** forward ĂĹĂÂĂÂĂÂÄšÂĂÂÄÂ§ĂÂĂÂĂĹÄšÂĂÂÄÂ§ĂÂĂÂ¨ĂĹĂÂĂÂĂÂĂÂÄšĹĽĂĹÄšĹžĂÂĂÂÄšĹĂÂĂÂĂÂÄšÂ forward ĂÂĂÂÄšÂÄÂ¤ĂÂ¸ÄšÂ nullĂÂĂÂĂÂ */
    private String dispatchedPath;
    private DispatcherType dispatcherType = DispatcherType.REQUEST;

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

    /** ĂĹĂÂÄšÄ˝ĂÂĂÂĂÂ query string ĂĹĂÂĂÂÄÂ§ĂÂĂÂĂÂĂÂÄšĹĽĂĹÄšĹžĂÂĂÂÄšĹĂÂÄÂ¤ÄšĹžĂÂ Servlet ĂÂĂÂĂÂ ĂĹĂÂ°ĂÂÄÂ¤ĂÂÄšĹşÄÂ§ĂÂĂÂ¨ĂÂĂÂĂÂ */
    public String getPath() {
        int query = uri.indexOf('?');
        String path = query >= 0 ? uri.substring(0, query) : uri;
        return path.isEmpty() ? "/" : path;
    }

    public String getContextPath() {
        return contextPath;
    }

    /** ĂĹĂÂÄšÄ˝ĂÂĂÂĂÂ Context ĂÂĂÂÄšĹĽĂĹÄšĹžĂÂĂĹĂÂĂÂÄÂ§ĂÂĂÂĂĹĂÂÄšÂ ÄÂ¤ĂÂĂÂĂÂĂÂÄšĹĽĂĹÄšĹžĂÂĂÂÄšĹĂÂÄÂ¤ÄšĹžĂÂĂÂÄšĹĽĂËĂĹÄšÂĂÂÄÂ§ĂÂĂÂ¨ĂĹĂÂĂÂÄĹ ĂÂĂÂ¨ĂÂĂÂĂÂ ĂĹĂÂ°ĂÂÄÂ¤ĂÂÄšĹşÄÂ§ĂÂĂÂ¨ĂÂĂÂĂÂ */
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

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath == null ? "" : contextPath;
    }

    public void setDispatchedPath(String dispatchedPath) {
        this.dispatchedPath = dispatchedPath;
    }

    public String getDispatchedPath() {
        return dispatchedPath;
    }

    public boolean isErrorDispatch() {
        return dispatcherType == DispatcherType.ERROR;
    }

    public DispatcherType getDispatcherType() {
        return dispatcherType;
    }

    public void setDispatcherType(DispatcherType dispatcherType) {
        this.dispatcherType = dispatcherType == null ? DispatcherType.REQUEST : dispatcherType;
    }

    public void bindContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
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

    /** ĂĹĂÂĂÂÄÂ§ÄšĹĂÂĂÂĂÂĂÂ ĂĹĂÂ°ĂÂĂĹĂÂ¤ĂÂĂĹĂÂÄšÂĂÂĂÂĂËÄÂ§ĂÂĂÂĂÂĂÂÄšĹĽĂĹÄšĹžĂÂĂÂÄšĹĂÂÄÂ¤ÄšĹžĂÂĂĹÄšÂĂÂ /app/* ĂĹĂÂÄšÄÄĹ ĂÂĂÂ /app/user ĂÂĂÂÄšÂÄÂ¤ĂÂ¸ÄšÂ /userĂÂĂÂĂÂ */
    public String getPathInfo() {
        return pathInfo;
    }

    public void setMapping(String servletPath, String pathInfo) {
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
     * HTTP/1.1 ÄĹ ÄšÄ˝ĂÂĂÂÄšËĂÂ¤ÄÂ¤ÄšĹşĂÂĂÂĂÂ´ÄšÄ˝ĂÂÄšĹĂÂHTTP/1.0 ÄĹ ÄšÄ˝ĂÂĂÂÄšËĂÂ¤ĂĹĂÂÄšÂÄĹ ĂÂĂÂ­ĂÂÄšĹĂÂĂÂÄšĹĽĂÂĂÂĂÂĂÂĂĹĂÂ¤ĂÂ´ Connection ĂĹĂÂÄšĹĽÄÂ¤ÄšÄ˝ĂËĂÂÄšÂĂÂÄÂ§ĂÂĂÂĂÂĂÂĂÂ
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

    /** ĂĹĂÂĂÂĂĹĂÂĂÂĂĹĂÂĂÂĂÂĂÂĂÂ°ĂĹĂÂÄšÂÄÂ¤ÄšĹşĂÂÄÂ§ĂÂĂÂÄÂ§ÄšĹĄÄšĹĄÄÂ¤ĂÂ¸ĂÂĂÂÄšĹĄĂÂĂĹĂÂÄšÂÄÂ§ĂÂĂÂ°ÄÂ§ĂÂĂÂĂĹĂÂÄšĹĂÂĂÂĂÂ */
    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getCookie(String name) {
        return cookies.get(name);
    }

    public void bindSession(SessionManager sessionManager, HttpResponse response) {
        this.sessionManager = sessionManager;
        this.response = response;
    }

    /**
     * ĂÂĂÂĂÂ Cookie ÄĹ ĂÂĂÂÄÂ§ĂÂĂÂ JSESSIONID ĂĹĂÂĂÂ SessionĂÂÄšĹĂÂĂÂĂÂĂÂĂÂĂÂĂÂĂĹĂÂ°ĂÂĂÂĂÂĂÂ°ĂĹÄšÄ˝ÄšÂĂĹÄšÄÄšÂ Set-CookieĂÂĂÂĂÂ
     */
    @Override
    public com.web.HttpSession getSession() {
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

    public void setBody(byte[] body) {
        this.body = body == null ? new byte[0] : body;
        parseFormBody();
    }

    /**
     * ĂÂĂÂĂÂĂÂĂÂĂÂ Content-Length ĂÂĂÂÄšÂĂĹĂÂĂÂÄÂ¤ĂÂĂÂ 0ĂÂĂÂĂÂÄĹ ĂÂĂÂĂÂÄšÂĂÂĂĹĂÂÄšĹĂÂÄšĹşĂÂĂĹĂÂĂÂ -1ĂÂĂÂĂÂ
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
     * ĂĹĂÂĂÂ¨ĂĹĂÂĂÂĂÂÄšĹĽÄšÄ˝ĂĹĂÂ­ĂÂĂÂĂÂĂÂÄĹ ĂÂĂÂĂÂĂÂÄšĹžĂÂÄšĹĽĂÂĂÂĂÂĂÂĂĹĂÂ¤ĂÂ´ÄÂ§ÄšÄ˝ĂÂĂÂĂÂĂÂÄÂ¤ĂÂĂÂÄÂ§ĂÂÄšËĂÂÄšĹĂÂÄÂ§ÄšĹĄÄšĹĄÄÂ¤ĂÂ¸ĂÂÄÂ¤ĂÂ¸ÄšÂ \\r ÄÂ§ĂÂĂÂÄÂ¤ĂÂ¸ĂÂĂÂĂÂ ĂÂĂÂÄšĹĂÂĂÂĂÂĂÂ
     * buffer ĂÂĂÂ­ĂÂ¤ĂÂĂÂÄšÂÄÂ¤ÄšÄ˝ĂÂĂÂĂÂÄšĹĽĂĹĂÂĂÂĂÂĂÂ¨ĂÂĂĹÄšĹĂÂĂÂÄšĹĂÂposition = ĂĹĂÂĂÂĂÂÄšĹĽÄšÄ˝ÄĹ ĂÂÄšĹşĂĹÄšÂÄšÂĂÂĂÂĂÂ
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
     * ĂÂĂÂ§ÄšÂĂÂĂÂĂÂĂÂÄšĹĽĂÂĂÂĂÂĂÂĂÂĂÂĂÂĂĹĂÂĂÂ HeaderĂÂĂÂĂÂĂÂĂÂ ÄšĹĂĹÄšĹĂÂÄÂ¤ĂÂ¸ĂÂĂĹÄšĹĽÄšÄĂÂĂÂÄšÂĂÂÄšĹşĂÂĂĹĂÂĂÂ nullĂÂĂÂĂÂ
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
