package com.darmoz.whatsapp.integration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AppIntegrationRequests {

    private static final String URL_REGEX = "https?://\\S+";
    private static final String HEADER_REGEX = "[A-Za-z0-9-]+";

    private AppIntegrationRequests() {
    }

    public record Create(
            @NotNull UUID applicationId,
            @NotBlank @Size(max = 150) String applicationName,
            @NotBlank @Size(max = 500) @Pattern(regexp = URL_REGEX, message = "tiene que ser una URL http(s)") String baseUrl,
            @Size(max = 100) @Pattern(regexp = HEADER_REGEX, message = "nombre de header invalido") String authHeaderName,
            String authHeaderValue,
            @Min(500) @Max(60_000) Integer timeoutMs
    ) {
    }

    /** Update parcial. authHeaderValue vacio = no tocar; clearAuthHeader=true lo borra. */
    public record Update(
            @Size(max = 150) String applicationName,
            @Size(max = 500) @Pattern(regexp = URL_REGEX, message = "tiene que ser una URL http(s)") String baseUrl,
            @Size(max = 100) @Pattern(regexp = HEADER_REGEX, message = "nombre de header invalido") String authHeaderName,
            String authHeaderValue,
            Boolean clearAuthHeader,
            @Min(500) @Max(60_000) Integer timeoutMs
    ) {
    }

    /** Prueba de conexion desde el dashboard: path relativo a la base URL. */
    public record Test(
            @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE") String method,
            @NotBlank @Size(max = 500) String path
    ) {
    }
}
