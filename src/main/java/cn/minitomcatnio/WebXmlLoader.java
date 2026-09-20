package cn.minitomcatnio;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 读最小 web.xml：servlet + servlet-mapping。
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
                servlets.put(name, newInstance(className));
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
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + webXml, e);
        }
    }

    private static Servlet newInstance(String className) throws Exception {
        Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
        if (!(instance instanceof Servlet servlet)) {
            throw new IllegalStateException(className + " is not a Servlet");
        }
        return servlet;
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            throw new IllegalStateException("Missing <" + tag + ">");
        }
        return nodes.item(0).getTextContent().trim();
    }
}
