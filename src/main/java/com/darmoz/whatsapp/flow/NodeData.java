package com.darmoz.whatsapp.flow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lectura tolerante del data (Map sin tipo) de un nodo. */
public final class NodeData {

    private NodeData() {
    }

    public static String string(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    public static List<Map<String, Object>> objects(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(NodeData::asStringKeyed)
                .toList();
    }

    public static Map<String, String> stringMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null && v != null && !String.valueOf(k).isBlank()) {
                    result.put(String.valueOf(k), String.valueOf(v));
                }
            });
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringKeyed(Object value) {
        return (Map<String, Object>) value;
    }
}
