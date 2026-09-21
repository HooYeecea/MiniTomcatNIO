package cn.minitomcatnio.loader;

import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 从当前 Context 的 docBase 读静态文件。Servlet 未命中时作为默认处理。
 * 目录请求按 welcome-file-list 依次查找。
 */
public class StaticResourceProcessor {

    public static void process(HttpRequest request, HttpResponse response, Context context) {
        Path docBase = context.getDocBase();
        String path = uriToPath(request.getPathWithinContext());
        Path file = resolveSafe(docBase, path);

        if (file == null) {
            response.sendError(403, "Forbidden");
            return;
        }

        if (Files.isDirectory(file)) {
            file = findWelcomeFile(file, context.getWelcomeFiles());
            if (file == null) {
                response.sendError(404, "Not Found");
                return;
            }
        }

        if (!Files.isRegularFile(file)) {
            response.sendError(404, "Not Found");
            return;
        }

        try {
            byte[] content = Files.readAllBytes(file);
            response.setStatus(200, "OK");
            response.setHeader("Content-Type", contentType(file.getFileName().toString()));
            response.setBody(content);
        } catch (IOException e) {
            response.sendError(500, "Internal Server Error");
        }
    }

    private static Path findWelcomeFile(Path directory, List<String> welcomeFiles) {
        Path root = directory.toAbsolutePath().normalize();
        for (String name : welcomeFiles) {
            if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
                continue;
            }
            Path candidate = root.resolve(name.trim()).normalize();
            if (!candidate.startsWith(root)) {
                continue;
            }
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String uriToPath(String uri) {
        int query = uri.indexOf('?');
        String path = query >= 0 ? uri.substring(0, query) : uri;
        path = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (path.isEmpty()) {
            return "/";
        }
        return path;
    }

    private static Path resolveSafe(Path docBase, String uriPath) {
        Path root = docBase.toAbsolutePath().normalize();
        String relative = uriPath.startsWith("/") ? uriPath.substring(1) : uriPath;
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) {
            return null;
        }
        return file;
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
