package com.darmoz.whatsapp.account;

import com.darmoz.whatsapp.common.crypto.SecretCipher;

import java.time.Instant;
import java.util.UUID;

/** Los secretos nunca salen en claro: solo enmascarados. */
public record MetaAccountResponse(
        UUID id,
        String name,
        String appId,
        String appSecretMasked,
        String accessTokenMasked,
        String wabaId,
        String graphApiVersion,
        boolean active,
        Instant lastValidatedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static MetaAccountResponse from(MetaAccount account) {
        return new MetaAccountResponse(
                account.getId(),
                account.getName(),
                account.getAppId(),
                SecretCipher.mask(account.getAppSecret()),
                SecretCipher.mask(account.getAccessToken()),
                account.getWabaId(),
                account.getGraphApiVersion(),
                account.isActive(),
                account.getLastValidatedAt(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
