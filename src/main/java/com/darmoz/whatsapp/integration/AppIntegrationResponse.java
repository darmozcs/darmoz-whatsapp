package com.darmoz.whatsapp.integration;

import com.darmoz.whatsapp.common.crypto.SecretCipher;

import java.time.Instant;
import java.util.UUID;

public record AppIntegrationResponse(
        UUID id,
        UUID applicationId,
        String applicationName,
        String baseUrl,
        String authHeaderName,
        String authHeaderValueMasked,
        int timeoutMs,
        Instant createdAt,
        Instant updatedAt
) {

    public static AppIntegrationResponse from(AppIntegration integration) {
        return new AppIntegrationResponse(
                integration.getId(),
                integration.getApplicationId(),
                integration.getApplicationName(),
                integration.getBaseUrl(),
                integration.getAuthHeaderName(),
                SecretCipher.mask(integration.getAuthHeaderValue()),
                integration.getTimeoutMs(),
                integration.getCreatedAt(),
                integration.getUpdatedAt()
        );
    }
}
