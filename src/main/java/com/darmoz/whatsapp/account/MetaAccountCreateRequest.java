package com.darmoz.whatsapp.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MetaAccountCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 64) String appId,
        @NotBlank String appSecret,
        @NotBlank String accessToken,
        @NotBlank @Size(max = 64) String wabaId,
        @Size(max = 10) String graphApiVersion,
        Boolean active
) {
}
