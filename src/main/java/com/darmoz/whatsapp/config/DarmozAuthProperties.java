package com.darmoz.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Los access tokens (RS256) de darmoz-auth se verifican aca con su llave
 * publica. Solo pasan los de la Application de sistema (la del dashboard)
 * que tienen el rol {@link #adminRole()}.
 */
@ConfigurationProperties(prefix = "darmoz-auth")
public record DarmozAuthProperties(
        /* PEM X.509 con la llave publica RSA con la que firma darmoz-auth. */
        String publicKeyPath,
        String issuer,
        /* UUID de la Application de sistema (claim applicationId). */
        String applicationId,
        String adminRole
) {
}
