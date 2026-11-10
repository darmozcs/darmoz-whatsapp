package com.darmoz.whatsapp.phonenumber;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** to: numero en formato internacional, solo digitos (ej. 59899123456). */
public record TestMessageRequest(
        @NotBlank @Pattern(regexp = "\\d{8,15}", message = "solo digitos, con codigo de pais") String to,
        @NotBlank @Size(max = 4096) String text
) {
}
