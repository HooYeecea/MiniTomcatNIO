package cn.minitomcatnio;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 按请求 URI 从 webroot 读静态文件。Servlet 未命中时作为默认处理。
 */
public class StaticResourceProcessor {

    static final Path WEB_ROOT = Paths.get("webroot").toAbsolutePath().normalize();

    public static void process(HttpRequest request, HttpResponse response) {
        String path = uriToPath(request.getUri());
        Path file = resolveSafe(path);

        if (file == null) {
            notFound(response, path, 403, "Forbidden");
            return;
        }

        if (Files.isDirectory(file)) {
            file = file.resolve("index.html");
        }

        if (!Files.isRegularFile(file)) {
            notFound(response, path, 404, "Not Found");
            return;
        }

        try {
            byte[] content = Files.readAllBytes(file);
            response.setStatus(200, "OK");
            response.setHeader("Content-Type", contentType(file.getFileName().toString()));
            response.setBody(content);
        } catch (IOException e) {
            response.setStatus(500, "Internal Server Error");
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            response.setBody("500 Internal Server Error");
        }
    }

    private static String uriToPath(String uri) {
        int query = uri.indexOf('?');
        String path = query >= 0 ? uri.substring(0, query) : uri;
        path = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (path.isEmpty() || "/".equals(path)) {
            return "/index.html";
        }
        return path;
    }

    /**
     * 把 URI 映射到 webroot 下的真实路径，拦住 .. 跳出目录。
     */
    private static Path resolveSafe(String uriPath) {
        String relative = uriPath.startsWith("/") ? uriPath.substring(1) : uriPath;
        Path file = WEB_ROOT.resolve(relative).normalize();
        if (!file.startsWith(WEB_ROOT)) {
            return null;
        }
        return file;
    }

    private static void notFound(HttpResponse response, String path, int status, String reason) {
        response.setStatus(status, reason);
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setBody(status + " " + reason + ": " + path);
    }

    private static String contentType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String ext = dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
        return switch (ext) {
            case "html", "htm" -> "text/html; charset=UTF-8";
            case "css" -> "text/css; charset=UTF-8";
            case "js" -> "application/javascript; charset=UTF-8";
            case "txt" -> "text/plain; charset=UTF-8";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }
}
