package com.darmoz.whatsapp.messagelog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageLogResponse(
        UUID id,
        UUID conversationId,
        UUID phoneNumberId,
        MessageDirection direction,
        String waMessageId,
        String contactWaId,
        String type,
        Map<String, Object> payload,
        MessageStatus status,
        String error,
        Instant createdAt,
        Instant updatedAt
) {

    public static MessageLogResponse from(MessageLog log) {
        return new MessageLogResponse(
                log.getId(),
                log.getConversationId(),
                log.getPhoneNumberId(),
                log.getDirection(),
                log.getWaMessageId(),
                log.getContactWaId(),
                log.getType(),
                log.getPayload(),
                log.getStatus(),
                log.getError(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
