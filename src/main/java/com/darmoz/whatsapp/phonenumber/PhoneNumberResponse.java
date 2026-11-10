package com.darmoz.whatsapp.phonenumber;

import java.time.Instant;
import java.util.UUID;

public record PhoneNumberResponse(
        UUID id,
        UUID accountId,
        String accountName,
        String phoneNumberId,
        String displayNumber,
        String verifiedName,
        UUID botId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static PhoneNumberResponse from(PhoneNumber phoneNumber) {
        return new PhoneNumberResponse(
                phoneNumber.getId(),
                phoneNumber.getAccount().getId(),
                phoneNumber.getAccount().getName(),
                phoneNumber.getPhoneNumberId(),
                phoneNumber.getDisplayNumber(),
                phoneNumber.getVerifiedName(),
                phoneNumber.getBotId(),
                phoneNumber.isActive(),
                phoneNumber.getCreatedAt(),
                phoneNumber.getUpdatedAt()
        );
    }
}
