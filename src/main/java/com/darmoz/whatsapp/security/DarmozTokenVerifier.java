package com.darmoz.whatsapp.security;

import com.darmoz.whatsapp.config.DarmozAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Verifica (nunca emite) los access tokens RS256 de darmoz-auth: firma,
 * issuer, expiracion y que sean de la Application de sistema (la del
 * dashboard). El rol lo exige SecurityConfig, asi un token valido sin el rol
 * recibe 403 y no 401.
 *
 * @throws JwtException si el token no es valido
 */
@Service
public class DarmozTokenVerifier {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_APPLICATION_ID = "applicationId";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";

    private final PublicKey verificationKey;
    private final DarmozAuthProperties properties;

    public DarmozTokenVerifier(DarmozAuthProperties properties) {
        if (isBlank(properties.publicKeyPath()) || isBlank(properties.applicationId())) {
            throw new IllegalStateException("Hay que configurar DARMOZ_AUTH_PUBLIC_KEY_PATH y DARMOZ_AUTH_APPLICATION_ID");
        }
        this.verificationKey = PemUtils.readPublicKey(Path.of(properties.publicKeyPath()));
        this.properties = properties;
    }

    public AuthenticatedAdmin verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String applicationId = claims.get(CLAIM_APPLICATION_ID, String.class);
        if (!properties.applicationId().equals(applicationId)) {
            throw new JwtException("applicationId inesperado: " + applicationId);
        }
        String type = claims.get(CLAIM_TYPE, String.class);
        if (type != null && !TYPE_ACCESS.equals(type)) {
            throw new JwtException("No es un access token: " + type);
        }

        List<?> roles = claims.get(CLAIM_ROLES, List.class);
        Set<String> roleNames = roles == null ? Set.of()
                : Set.copyOf(roles.stream().map(String::valueOf).toList());
        return new AuthenticatedAdmin(UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class), roleNames);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
