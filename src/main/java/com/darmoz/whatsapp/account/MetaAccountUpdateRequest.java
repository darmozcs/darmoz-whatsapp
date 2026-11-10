package com.darmoz.whatsapp.account;

import jakarta.validation.constraints.Size;

/** Update parcial: lo que viene null (o vacio en los secretos) no se toca. */
public record MetaAccountUpdateRequest(
        @Size(max = 150) String name,
        @Size(max = 64) String appId,
        String appSecret,
        String accessToken,
        @Size(max = 64) String wabaId,
        @Size(max = 10) String graphApiVersion,
        Boolean active
) {
}
