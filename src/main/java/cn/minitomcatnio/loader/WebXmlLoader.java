package cn.minitomcatnio.loader;

import cn.minitomcatnio.container.Context;
import com.web.DispatcherType;
import com.web.Filter;
import com.web.Servlet;
import cn.minitomcatnio.servlet.ServletContextListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 读最小 web.xml：servlet / filter / error-page / welcome-file。
 */
public class WebXmlLoader {

    public static void load(Context context) {
        Path webXml = context.getDocBase().resolve("WEB-INF").resolve("web.xml");
        if (!Files.isRegularFile(webXml)) {
            return;
        }

        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(webXml.toFile());
            document.getDocumentElement().normalize();

            NodeList listenerNodes = document.getElementsByTagName("listener");
            for (int i = 0; i < listenerNodes.getLength(); i++) {
                Element listener = (Element) listenerNodes.item(i);
                String className = text(listener, "listener-class");
                context.addListener(newInstance(context, className, ServletContextListener.class));
            }

            Map<String, Servlet> servlets = new LinkedHashMap<>();
            Map<String, Map<String, String>> initParams = new LinkedHashMap<>();
            NodeList servletNodes = document.getElementsByTagName("servlet");
            for (int i = 0; i < servletNodes.getLength(); i++) {
                Element servlet = (Element) servletNodes.item(i);
                String name = text(servlet, "servlet-name");
                String className = text(servlet, "servlet-class");
                servlets.put(name, newInstance(context, className, Servlet.class));
                initParams.put(name, readInitParams(servlet));
            }

            NodeList mappingNodes = document.getElementsByTagName("servlet-mapping");
            for (int i = 0; i < mappingNodes.getLength(); i++) {
                Element mapping = (Element) mappingNodes.item(i);
                String name = text(mapping, "servlet-name");
                String pattern = text(mapping, "url-pattern");
                Servlet servlet = servlets.get(name);
                if (servlet == null) {
                    throw new IllegalStateException("Unknown servlet-name in mapping: " + name);
                }
                context.addServlet(pattern, servlet, initParams.get(name));
            }

            Map<String, Filter> filters = new LinkedHashMap<>();
            NodeList filterNodes = document.getElementsByTagName("filter");
            for (int i = 0; i < filterNodes.getLength(); i++) {
                Element filter = (Element) filterNodes.item(i);
                String name = text(filter, "filter-name");
                String className = text(filter, "filter-class");
                filters.put(name, newInstance(context, className, Filter.class));
            }

            NodeList filterMappingNodes = document.getElementsByTagName("filter-mapping");
            for (int i = 0; i < filterMappingNodes.getLength(); i++) {
                Element mapping = (Element) filterMappingNodes.item(i);
                String name = text(mapping, "filter-name");
                String pattern = text(mapping, "url-pattern");
                Filter filter = filters.get(name);
                if (filter == null) {
                    throw new IllegalStateException("Unknown filter-name in mapping: " + name);
                }
                context.addFilter(pattern, filter, parseDispatchers(mapping));
            }

            NodeList contextParamNodes = document.getElementsByTagName("context-param");
            for (int i = 0; i < contextParamNodes.getLength(); i++) {
                Element param = (Element) contextParamNodes.item(i);
                context.setInitParameter(text(param, "param-name"), text(param, "param-value"));
            }

            NodeList errorPageNodes = document.getElementsByTagName("error-page");
            for (int i = 0; i < errorPageNodes.getLength(); i++) {
                Element errorPage = (Element) errorPageNodes.item(i);
                int status = Integer.parseInt(text(errorPage, "error-code"));
                String location = text(errorPage, "location");
                context.addErrorPage(status, location);
            }

            NodeList welcomeLists = document.getElementsByTagName("welcome-file-list");
            if (welcomeLists.getLength() > 0) {
                context.clearWelcomeFiles();
                Element list = (Element) welcomeLists.item(0);
                NodeList files = list.getElementsByTagName("welcome-file");
                for (int i = 0; i < files.getLength(); i++) {
                    context.addWelcomeFile(files.item(i).getTextContent());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + webXml, e);
        }
    }

    private static Map<String, String> readInitParams(Element servlet) {
        Map<String, String> params = new LinkedHashMap<>();
        NodeList paramNodes = servlet.getElementsByTagName("init-param");
        for (int i = 0; i < paramNodes.getLength(); i++) {
            Element param = (Element) paramNodes.item(i);
            params.put(text(param, "param-name"), text(param, "param-value"));
        }
        return params;
    }

    private static Set<DispatcherType> parseDispatchers(Element mapping) {
        EnumSet<DispatcherType> dispatchers = EnumSet.noneOf(DispatcherType.class);
        NodeList nodes = mapping.getElementsByTagName("dispatcher");
        for (int i = 0; i < nodes.getLength(); i++) {
            String value = nodes.item(i).getTextContent().trim();
            dispatchers.add(DispatcherType.valueOf(value));
        }
        if (dispatchers.isEmpty()) {
            dispatchers.add(DispatcherType.REQUEST);
        }
        return dispatchers;
    }

    private static <T> T newInstance(Context context, String className, Class<T> type) throws Exception {
        Class<?> clazz = Class.forName(className, true, context.getClassLoader());
        System.out.println("load " + className + " via " + clazz.getClassLoader().getClass().getSimpleName());
        Object instance = clazz.getDeclaredConstructor().newInstance();
        if (!type.isInstance(instance)) {
            throw new IllegalStateException(className + " is not a " + type.getSimpleName());
        }
        return type.cast(instance);
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            throw new IllegalStateException("Missing <" + tag + ">");
        }
        return nodes.item(0).getTextContent().trim();
    }
}
