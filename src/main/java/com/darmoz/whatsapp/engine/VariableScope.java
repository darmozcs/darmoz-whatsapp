package com.darmoz.whatsapp.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resuelve {{path}} en los flujos:
 * <ul>
 *   <li>contact.waId / contact.name: el contacto de WhatsApp</li>
 *   <li>bot.clave: las static_variables del bot</li>
 *   <li>cualquier otro: variables de la conversacion, con puntos para entrar en
 *       objetos (api.cliente.nombre) e indices para listas (api.items.0.total)</li>
 * </ul>
 */
public record VariableScope(String contactWaId, String contactName, Map<String, String> botVariables,
                            Map<String, Object> variables) {

    public Object resolve(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.trim().split("\\.");
        Object current;
        int index = 1;
        switch (parts[0]) {
            case "contact" -> {
                if (parts.length != 2) {
                    return null;
                }
                return switch (parts[1]) {
                    case "waId", "phone" -> contactWaId;
                    case "name" -> contactName;
                    default -> null;
                };
            }
            case "bot" -> current = botVariables;
            default -> {
                current = variables.get(parts[0]);
            }
        }
        if ("bot".equals(parts[0]) && parts.length == 1) {
            return null;
        }
        for (; index < parts.length && current != null; index++) {
            current = child(current, parts[index]);
        }
        return current;
    }

    public String resolveAsText(String path) {
        return toText(resolve(path));
    }

    public static String toText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double || value instanceof Float) {
            BigDecimal decimal = BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros();
            return decimal.scale() < 0 ? decimal.setScale(0).toPlainString() : decimal.toPlainString();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private static Object child(Object current, String key) {
        if (current instanceof Map<?, ?> map) {
            return map.get(key);
        }
        if (current instanceof List<?> list) {
            try {
                int position = Integer.parseInt(key);
                return position >= 0 && position < list.size() ? list.get(position) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
