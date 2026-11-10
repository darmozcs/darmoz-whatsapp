package com.darmoz.whatsapp.engine;

import java.time.Instant;
import java.util.Map;

/**
 * Mensaje entrante ya normalizado desde el webhook.
 *
 * @param text    lo que escribio (o el titulo del boton/fila que toco)
 * @param replyId id del boton o fila de un mensaje interactivo, si respondio a uno
 * @param raw     el objeto message tal cual vino de Meta (va al log)
 */
public record InboundMessage(String waMessageId, String from, String contactName, String type, String text,
                             String replyId, Instant timestamp, Map<String, Object> raw) {
}
