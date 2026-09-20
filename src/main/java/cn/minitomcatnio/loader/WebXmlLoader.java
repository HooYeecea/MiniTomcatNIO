package cn.minitomcatnio.loader;

import cn.minitomcatnio.container.Context;
import cn.minitomcatnio.servlet.Filter;
import cn.minitomcatnio.servlet.Servlet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 读最小 web.xml：servlet / filter 及其 mapping。
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

            Map<String, Servlet> servlets = new LinkedHashMap<>();
            NodeList servletNodes = document.getElementsByTagName("servlet");
            for (int i = 0; i < servletNodes.getLength(); i++) {
                Element servlet = (Element) servletNodes.item(i);
                String name = text(servlet, "servlet-name");
                String className = text(servlet, "servlet-class");
                servlets.put(name, newInstance(context, className, Servlet.class));
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
                context.addServlet(pattern, servlet);
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
                context.addFilter(pattern, filter);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + webXml, e);
        }
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
