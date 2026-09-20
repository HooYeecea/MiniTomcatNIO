package cn.minitomcatnio.loader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 每个 Context 自己的 ClassLoader：先查 WEB-INF/classes 和 WEB-INF/lib，
 * 服务器自身的包仍走 parent，避免应用盖住容器类。
 */
public class WebappClassLoader extends URLClassLoader {

    public WebappClassLoader(Path docBase, ClassLoader parent) {
        super(toUrls(docBase), parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }

            if (isServerClass(name)) {
                return super.loadClass(name, resolve);
            }

            try {
                Class<?> local = findClass(name);
                if (resolve) {
                    resolveClass(local);
                }
                return local;
            } catch (ClassNotFoundException ignored) {
                return super.loadClass(name, resolve);
            }
        }
    }

    private static boolean isServerClass(String name) {
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")) {
            return true;
        }
        if (!name.startsWith("cn.minitomcatnio.")) {
            return false;
        }
        // demo 应用类交给 WEB-INF 优先；其余容器包走 parent。
        return !name.startsWith("cn.minitomcatnio.demo.");
    }

    private static URL[] toUrls(Path docBase) {
        List<URL> urls = new ArrayList<>();
        Path classes = docBase.resolve("WEB-INF").resolve("classes");
        if (Files.isDirectory(classes)) {
            urls.add(toUrl(classes));
        }
        Path lib = docBase.resolve("WEB-INF").resolve("lib");
        if (Files.isDirectory(lib)) {
            try (DirectoryStream<Path> jars = Files.newDirectoryStream(lib, "*.jar")) {
                for (Path jar : jars) {
                    urls.add(toUrl(jar));
                }
            } catch (IOException ignored) {
            }
        }
        return urls.toArray(URL[]::new);
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad classpath entry: " + path, e);
        }
    }
}
