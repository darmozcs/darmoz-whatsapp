package com.darmoz.whatsapp.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * to: numero en formato internacional, solo digitos (ej. 59899123456).
 * phoneNumberId: id interno (wa_phone_number) del numero que envia. Solo hace falta
 * si la aplicacion tiene mas de un numero activo.
 */
public record SendMessageRequest(
        @NotBlank @Pattern(regexp = "\\d{8,15}", message = "solo digitos, con codigo de pais") String to,
        @NotBlank @Size(max = 4096) String text,
        UUID phoneNumberId
) {
}
