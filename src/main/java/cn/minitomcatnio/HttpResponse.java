package cn.minitomcatnio;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次 HTTP 响应。业务只填状态和正文，编码成字节交给 NIO 去写。
 */
public class HttpResponse {

    private int status = 200;
    private String reason = "OK";
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    public HttpResponse() {
        headers.put("Content-Type", "text/plain; charset=UTF-8");
    }

    public void setStatus(int status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public void setBody(String text) {
        setBody(text.getBytes(StandardCharsets.UTF_8));
    }

    public void setBody(byte[] body) {
        this.body = body == null ? new byte[0] : body;
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
        headerBuilder.append("\r\n");

        byte[] headerBytes = headerBuilder.toString().getBytes(StandardCharsets.ISO_8859_1);
        ByteBuffer buffer = ByteBuffer.allocate(headerBytes.length + body.length);
        buffer.put(headerBytes);
        buffer.put(body);
        buffer.flip();
        return buffer;
    }
}
