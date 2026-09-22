package cn.minitomcatnio.http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次 HTTP 响应。业务只填状态和正文，编码成字节交给 NIO 去写。
 */
public class HttpResponse implements com.web.HttpResponse {

    private int status = 200;
    private String reason = "OK";
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final List<String> setCookies = new ArrayList<>();
    private byte[] body = new byte[0];
    private boolean error;

    public HttpResponse() {
        headers.put("Content-Type", "text/plain; charset=UTF-8");
    }

    @Override
    public void setStatus(int status) {
        setStatus(status, defaultReason(status));
    }

    @Override
    public void setStatus(int status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public boolean isError() {
        return error;
    }

    /** 标记本次响应为错误。容器若配了对应 error-page，会改用那个页面的正文。 */
    public void sendError(int status, String reason) {
        this.status = status;
        this.reason = reason == null || reason.isEmpty() ? "Error" : reason;
        this.error = true;
        headers.put("Content-Type", "text/plain; charset=UTF-8");
        setBody(this.status + " " + this.reason);
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public void addCookie(String name, String value) {
        setCookies.add(name + "=" + value);
    }

    public void setBody(String text) {
        setBody(text.getBytes(StandardCharsets.UTF_8));
    }

    public void setBody(byte[] body) {
        this.body = body == null ? new byte[0] : body;
    }

    public byte[] getBody() {
        return body;
    }

    public void appendBody(byte[] extra) {
        if (extra == null || extra.length == 0) {
            return;
        }
        byte[] merged = new byte[body.length + extra.length];
        System.arraycopy(body, 0, merged, 0, body.length);
        System.arraycopy(extra, 0, merged, body.length, extra.length);
        body = merged;
    }

    /** forward 前清空本次响应；已写下的 Set-Cookie 保留。 */
    public void reset() {
        status = 200;
        reason = "OK";
        headers.clear();
        headers.put("Content-Type", "text/plain; charset=UTF-8");
        body = new byte[0];
        error = false;
    }

    /**
     * 编码成一条完整的 HTTP/1.1 报文，buffer 已 flip，可直接 channel.write。
     */
    public ByteBuffer toByteBuffer() {
        headers.put("Content-Length", String.valueOf(body.length));

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            headerBuilder.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        for (String cookie : setCookies) {
            headerBuilder.append("Set-Cookie: ").append(cookie).append("\r\n");
        }
        headerBuilder.append("\r\n");

        byte[] headerBytes = headerBuilder.toString().getBytes(StandardCharsets.ISO_8859_1);
        ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + body.length);
        buffer.put(headerBytes);
        buffer.put(body);
        buffer.flip();
        return buffer;
    }

    private static String defaultReason(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Status";
        };
    }
}
