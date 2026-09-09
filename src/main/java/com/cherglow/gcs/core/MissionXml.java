package com.cherglow.gcs.core;

import com.cherglow.gcs.model.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * 任务文件 XML 编解码与校验。
 */
public final class MissionXml {

    public static final int FORMAT_VERSION = 1;

    private MissionXml() {
    }

    public record MissionWaypoint(int id, double lat, double lon, double altM, int staySec,
                                  Waypoint.Action action, int priority, Waypoint.Role role) {
    }

    public record MissionFile(String name, int version, List<MissionWaypoint> waypoints) {
    }

    public static void save(Path file, String name, Collection<Waypoint> waypoints) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        DocumentBuilder builder = newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("mission");
        root.setAttribute("version", String.valueOf(FORMAT_VERSION));
        root.setAttribute("waypointCount", String.valueOf(waypoints.size()));
        doc.appendChild(root);

        Element nameEl = doc.createElement("name");
        nameEl.setTextContent(name == null ? "" : name);
        root.appendChild(nameEl);

        Element listEl = doc.createElement("waypoints");
        root.appendChild(listEl);

        for (Waypoint wp : waypoints) {
            Element item = doc.createElement("waypoint");
            item.setAttribute("id", String.valueOf(wp.getId()));
            item.setAttribute("lat", String.valueOf(wp.getLat()));
            item.setAttribute("lon", String.valueOf(wp.getLon()));
            item.setAttribute("alt", String.valueOf(wp.getAltM()));
            item.setAttribute("stay", String.valueOf(wp.getStaySec()));
            item.setAttribute("action", wp.getAction().name());
            item.setAttribute("priority", String.valueOf(wp.getPriority()));
            item.setAttribute("role", wp.getRole().name());
            listEl.appendChild(item);
        }

        writeDocument(doc, file);
        load(file);
    }

    public static MissionFile load(Path file) throws IOException {
        Document doc = parse(file);
        Element root = doc.getDocumentElement();
        if (root == null || !"mission".equals(root.getTagName())) {
            throw new IOException("任务 XML 根节点必须是 <mission>");
        }

        int version = requiredIntAttr(root, "version");
        if (version != FORMAT_VERSION) {
            throw new IOException("不支持的任务 XML 版本: " + version);
        }

        String name = childText(root, "name");
        Element listEl = childElement(root, "waypoints");
        List<MissionWaypoint> waypoints = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        NodeList nodes = listEl.getChildNodes();
        int fallbackId = 1;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element item = (Element) node;
            if (!"waypoint".equals(item.getTagName())) {
                throw new IOException("waypoints 中只能包含 <waypoint> 节点");
            }
            int id = optionalIntAttr(item, "id", fallbackId);
            fallbackId = id + 1;
            double lat = requiredDoubleAttr(item, "lat");
            double lon = requiredDoubleAttr(item, "lon");
            double alt = optionalDoubleAttr(item, "alt", 0.0);
            int stay = optionalIntAttr(item, "stay", 0);
            int priority = optionalIntAttr(item, "priority", 0);
            Waypoint.Action action = parseAction(requiredAttr(item, "action"));
            Waypoint.Role role = parseRole(requiredAttr(item, "role"));
            if (!ids.add(id)) {
                throw new IOException("存在重复的航点 id: " + id);
            }
            waypoints.add(new MissionWaypoint(id, lat, lon, alt, stay, action, priority, role));
        }

        int declaredCount = requiredIntAttr(root, "waypointCount");
        if (declaredCount != waypoints.size()) {
            throw new IOException("waypointCount 与实际航点数量不一致");
        }

        return new MissionFile(name, version, waypoints);
    }

    private static Document parse(Path file) throws IOException {
        DocumentBuilder builder = newDocumentBuilder();
        try (InputStream in = Files.newInputStream(file)) {
            return builder.parse(in);
        } catch (SAXException e) {
            throw new IOException("XML 语法错误: " + e.getMessage(), e);
        }
    }

    private static void writeDocument(Document doc, Path file) throws IOException {
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            try (OutputStream out = Files.newOutputStream(file)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
        } catch (TransformerException e) {
            throw new IOException("XML 写入失败: " + e.getMessage(), e);
        }
    }

    private static DocumentBuilder newDocumentBuilder() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return builder;
        } catch (ParserConfigurationException e) {
            throw new IOException("XML 解析器初始化失败: " + e.getMessage(), e);
        }
    }

    private static Element childElement(Element parent, String name) throws IOException {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        throw new IOException("缺少 <" + name + "> 节点");
    }

    private static String childText(Element parent, String name) throws IOException {
        Element child = childElement(parent, name);
        return child.getTextContent();
    }

    private static String requiredAttr(Element el, String name) throws IOException {
        String v = el.getAttribute(name);
        if (v == null || v.isBlank()) {
            throw new IOException("缺少必填属性: " + name);
        }
        return v;
    }

    private static int requiredIntAttr(Element el, String name) throws IOException {
        String v = requiredAttr(el, name);
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IOException("属性 " + name + " 不是有效整数: " + v, e);
        }
    }

    private static int optionalIntAttr(Element el, String name, int defaultValue) throws IOException {
        String v = el.getAttribute(name);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IOException("属性 " + name + " 不是有效整数: " + v, e);
        }
    }

    private static double requiredDoubleAttr(Element el, String name) throws IOException {
        String v = requiredAttr(el, name);
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            throw new IOException("属性 " + name + " 不是有效数值: " + v, e);
        }
    }

    private static double optionalDoubleAttr(Element el, String name, double defaultValue) throws IOException {
        String v = el.getAttribute(name);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            throw new IOException("属性 " + name + " 不是有效数值: " + v, e);
        }
    }

    private static Waypoint.Action parseAction(String value) throws IOException {
        Waypoint.Action action = Waypoint.Action.from(value);
        if (action == null) {
            throw new IOException("未知航点动作: " + value);
        }
        return action;
    }

    private static Waypoint.Role parseRole(String value) throws IOException {
        Waypoint.Role role = Waypoint.Role.from(value);
        if (role == null) {
            throw new IOException("未知航点角色: " + value);
        }
        return role;
    }
}
