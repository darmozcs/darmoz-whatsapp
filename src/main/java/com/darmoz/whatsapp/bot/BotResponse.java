package com.darmoz.whatsapp.bot;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BotResponse(
        UUID id,
        String name,
        UUID applicationId,
        boolean active,
        UUID entryFlowId,
        String fallbackMessage,
        int sessionTimeoutMinutes,
        BusinessHours businessHours,
        String outOfHoursMessage,
        Map<String, String> staticVariables,
        Instant createdAt,
        Instant updatedAt
) {

    public static BotResponse from(Bot bot) {
        return new BotResponse(
                bot.getId(),
                bot.getName(),
                bot.getApplicationId(),
                bot.isActive(),
                bot.getEntryFlowId(),
                bot.getFallbackMessage(),
                bot.getSessionTimeoutMinutes(),
                bot.getBusinessHours(),
                bot.getOutOfHoursMessage(),
                bot.getStaticVariables(),
                bot.getCreatedAt(),
                bot.getUpdatedAt()
        );
    }
}
