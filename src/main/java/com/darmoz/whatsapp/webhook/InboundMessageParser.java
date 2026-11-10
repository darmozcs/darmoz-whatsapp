package com.darmoz.whatsapp.webhook;

import com.darmoz.whatsapp.engine.InboundMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Normaliza un objeto message de Meta: texto, respuesta a botones/listas o tipo no soportado. */
final class InboundMessageParser {

    private InboundMessageParser() {
    }

    static InboundMessage parse(Map<String, Object> raw, List<WebhookPayload.Contact> contacts) {
        String from = string(raw.get("from"));
        String type = string(raw.get("type"));
        String text = null;
        String replyId = null;

        switch (type == null ? "" : type) {
            case "text" -> text = string(child(raw, "text").get("body"));
            case "interactive" -> {
                Map<String, Object> interactive = child(raw, "interactive");
                Map<String, Object> reply = child(interactive,
                        "list_reply".equals(interactive.get("type")) ? "list_reply" : "button_reply");
                replyId = string(reply.get("id"));
                text = string(reply.get("title"));
            }
            // Boton de un template (quick reply): payload hace de id.
            case "button" -> {
                Map<String, Object> button = child(raw, "button");
                replyId = string(button.get("payload"));
                text = string(button.get("text"));
            }
            default -> {
                // imagen, audio, ubicacion...: el flujo lo ve como texto vacio.
            }
        }

        String contactName = contacts == null ? null : contacts.stream()
                .filter(contact -> from != null && from.equals(contact.waId()) && contact.profile() != null)
                .map(contact -> contact.profile().name())
                .findFirst().orElse(null);

        return new InboundMessage(string(raw.get("id")), from, contactName, type, text, replyId,
                parseTimestamp(raw.get("timestamp")), raw);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Instant parseTimestamp(Object value) {
        try {
            return value == null ? Instant.now() : Instant.ofEpochSecond(Long.parseLong(String.valueOf(value)));
        } catch (NumberFormatException e) {
            return Instant.now();
        }
    }
}
