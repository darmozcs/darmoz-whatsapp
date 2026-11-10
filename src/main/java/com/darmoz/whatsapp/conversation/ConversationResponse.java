package com.darmoz.whatsapp.conversation;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID phoneNumberId,
        String contactWaId,
        String contactName,
        UUID currentFlowId,
        String currentNodeId,
        Map<String, Object> variables,
        ConversationStatus status,
        Instant lastInboundAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getPhoneNumberId(),
                conversation.getContactWaId(),
                conversation.getContactName(),
                conversation.getCurrentFlowId(),
                conversation.getCurrentNodeId(),
                conversation.getVariables(),
                conversation.getStatus(),
                conversation.getLastInboundAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
