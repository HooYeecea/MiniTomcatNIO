package cn.minitomcatnio.http;

import cn.minitomcatnio.servlet.ApplicationDispatcher;
import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.session.HttpSession;
import cn.minitomcatnio.servlet.RequestDispatcher;
import cn.minitomcatnio.servlet.Servlet;
import cn.minitomcatnio.session.SessionManager;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ă¤Â¸ÂÄĹšÄ HTTP ÄĹťËÄÄÂÄĹşÂÄĹťËÄÄÂÄÄÂÄÂÂHeaderÄÂÂbodyÄÂÂÄşÂÂÄÂÂ°ÄşÂÂ CookieÄÂÂ
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
    /** forward ÄşÂÂÄĹÂĂ§ÂÂÄşĹÂĂ§ÂÂ¨ÄşÂÂÄËĹťÄşĹžÂÄĹşÂÄÂĹ forward ÄÂĹĂ¤Â¸Ĺ nullÄÂÂ */
    private String dispatchedPath;
    /** 正在派发 error-page，避免错误页自己再抛异常时死循环。 */
    private boolean errorDispatch;

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

    /** ÄşÂĹĽÄÂÂ query string ÄşÂÂĂ§ÂÂÄËĹťÄşĹžÂÄĹşÂĂ¤ĹžÂ Servlet ÄÂÂ ÄşÂ°ÂĂ¤ËĹźĂ§ÂÂ¨ÄÂÂ */
    public String getPath() {
        int query = uri.indexOf('?');
        String path = query >= 0 ? uri.substring(0, query) : uri;
        return path.isEmpty() ? "/" : path;
    }

    public String getContextPath() {
        return contextPath;
    }

    /** ÄşÂĹĽÄÂÂ Context ÄËĹťÄşĹžÂÄşÂÂĂ§ÂÂÄşÂĹ Ă¤ËÂÄËĹťÄşĹžÂÄĹşÂĂ¤ĹžÂÄĹťÄ˝ÄşĹÂĂ§ÂÂ¨ÄşÂÂĂŠÂÂ¨ÄÂÂ ÄşÂ°ÂĂ¤ËĹźĂ§ÂÂ¨ÄÂÂ */
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
        return errorDispatch;
    }

    public void setErrorDispatch(boolean errorDispatch) {
        this.errorDispatch = errorDispatch;
    }

    public void bindContext(Context context) {
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

    /** ÄşÂÂĂ§ĹşÂÄÂÂ ÄşÂ°ÂÄşÂ¤ÂÄşÂĹÄÂÄ˝Ă§ÂÂÄËĹťÄşĹžÂÄĹşÂĂ¤ĹžÂÄşĹÂ /app/* ÄşÂĹĄĂŠÂÂ /app/user ÄÂĹĂ¤Â¸Ĺ /userÄÂÂ */
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
     * HTTP/1.1 ĂŠĹĽÂÄĹ˝Â¤Ă¤ĹźÂÄÂ´ĹĽÄĹşÂHTTP/1.0 ĂŠĹĽÂÄĹ˝Â¤ÄşÂĹĂŠÂÂ­ÄĹşÂÄĹťËÄÄÂÄşÂ¤Â´ Connection ÄşÂĹťĂ¤ĹĽÄ˝ÄĹÂĂ§ÂÂÄÂÂ
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

    /** ÄşÂÂÄşÂÂÄşÂÂÄÂÂ°ÄşÂĹĂ¤ĹźÂĂ§ÂÂĂ§ĹšĹšĂ¤Â¸ÂÄĹšÄÄşÂĹĂ§ÂÂ°Ă§ÂÂÄşÂĹşÄÂÂ */
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
     * ÄÂÂ Cookie ĂŠÂÂĂ§ÂÂ JSESSIONID ÄşÂÂ SessionÄĹşÂÄËÄÄÂÂÄşÂ°ÄÄÂÂ°ÄşĹĽĹÄşĹĄĹ Set-CookieÄÂÂ
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

    public void setBody(byte[] body) {
        this.body = body == null ? new byte[0] : body;
        parseFormBody();
    }

    /**
     * ÄËÄÄÂÂ Content-Length ÄÂĹÄşËÂĂ¤ËÂ 0ÄÂÂĂŠÂÂÄĹÂÄşÂĹşÄĹźÂÄşÂÂ -1ÄÂÂ
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
     * ÄşÂÂ¨ÄşËËÄĹťĹĽÄşÂ­ÂÄÂÂĂŠÂÂÄÂĹžÄĹťËÄÄÂÄşÂ¤Â´Ă§ĹĽÂÄÂÂĂ¤ËÂĂ§ËĹ˝ÄĹşÂĂ§ĹšĹšĂ¤Â¸ÂĂ¤Â¸Ĺ \\r Ă§ÂÂĂ¤Â¸ÂÄÂ ÂÄĹşÂÄÂÂ
     * buffer ÄÂ­Â¤ÄÂĹĂ¤ĹĽÂÄÂĹťÄşÂÂÄÂ¨ÄÄşĹşÂÄĹşÂposition = ÄşËËÄĹťĹĽĂŠÂĹźÄşĹĹÄÂÂ
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
     * ÄÂ§ĹÄÂÂÄĹťËÄÄÂÄÄÂÄşÂÂ HeaderÄÂÂÄÂ ĹşÄşĹşÂĂ¤Â¸ÂÄşĹťĹĄÄÂĹÄĹźÂÄşÂÂ nullÄÂÂ
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
