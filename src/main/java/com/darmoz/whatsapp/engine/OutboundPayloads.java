package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.flow.NodeData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Arma el payload de la Cloud API para cada nodo que manda algo, con los textos ya renderizados. */
public final class OutboundPayloads {

    private OutboundPayloads() {
    }

    public static Map<String, Object> text(String body) {
        return Map.of("type", "text", "text", Map.of("body", body, "preview_url", false));
    }

    /** message: archivo (con el texto como caption si no hay caption) o solo texto. */
    public static Map<String, Object> message(Map<String, Object> data, VariableScope scope) {
        String text = TemplateRenderer.render(NodeData.string(data, "text"), scope);
        String mediaUrl = TemplateRenderer.render(NodeData.string(data, "mediaUrl"), scope);
        if (mediaUrl == null) {
            return text(text);
        }
        String mediaType = "document".equals(NodeData.string(data, "mediaType")) ? "document" : "image";
        Map<String, Object> media = new LinkedHashMap<>();
        media.put("link", mediaUrl);
        String caption = TemplateRenderer.render(NodeData.string(data, "caption"), scope);
        if (caption != null || text != null) {
            media.put("caption", caption != null ? caption : text);
        }
        String filename = NodeData.string(data, "filename");
        if ("document".equals(mediaType) && filename != null) {
            media.put("filename", filename);
        }
        return Map.of("type", mediaType, mediaType, media);
    }

    public static Map<String, Object> buttons(Map<String, Object> data, VariableScope scope) {
        List<Map<String, Object>> buttons = new ArrayList<>();
        for (Map<String, Object> button : NodeData.objects(data, "buttons")) {
            buttons.add(Map.of("type", "reply", "reply", Map.of(
                    "id", NodeData.string(button, "id"),
                    "title", TemplateRenderer.render(NodeData.string(button, "title"), scope))));
        }
        return interactive("button", data, scope, Map.of("buttons", buttons));
    }

    public static Map<String, Object> list(Map<String, Object> data, VariableScope scope) {
        List<Map<String, Object>> sections = new ArrayList<>();
        for (Map<String, Object> section : NodeData.objects(data, "sections")) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, Object> row : NodeData.objects(section, "rows")) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", NodeData.string(row, "id"));
                item.put("title", TemplateRenderer.render(NodeData.string(row, "title"), scope));
                String description = NodeData.string(row, "description");
                if (description != null) {
                    item.put("description", TemplateRenderer.render(description, scope));
                }
                rows.add(item);
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            String title = NodeData.string(section, "title");
            if (title != null) {
                entry.put("title", title);
            }
            entry.put("rows", rows);
            sections.add(entry);
        }
        return interactive("list", data, scope, Map.of(
                "button", NodeData.string(data, "buttonText"),
                "sections", sections));
    }

    private static Map<String, Object> interactive(String type, Map<String, Object> data, VariableScope scope,
                                                   Map<String, Object> action) {
        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", type);
        String header = TemplateRenderer.render(NodeData.string(data, "header"), scope);
        if (header != null) {
            interactive.put("header", Map.of("type", "text", "text", header));
        }
        interactive.put("body", Map.of("text", TemplateRenderer.render(NodeData.string(data, "text"), scope)));
        String footer = TemplateRenderer.render(NodeData.string(data, "footer"), scope);
        if (footer != null) {
            interactive.put("footer", Map.of("text", footer));
        }
        interactive.put("action", action);
        return Map.of("type", "interactive", "interactive", interactive);
    }
}
