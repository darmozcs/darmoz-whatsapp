package com.darmoz.whatsapp.account;

import java.time.Instant;

public record AccountValidationResponse(boolean valid, String wabaName, String error, Instant validatedAt) {
}
