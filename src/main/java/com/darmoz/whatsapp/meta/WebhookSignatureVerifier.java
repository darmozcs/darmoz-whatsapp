package com.darmoz.whatsapp.meta;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * X-Hub-Signature-256: "sha256=" + HMAC-SHA256 del body crudo con el App
 * Secret. Se compara en tiempo constante.
 */
public final class WebhookSignatureVerifier {

    private static final String PREFIX = "sha256=";

    private WebhookSignatureVerifier() {
    }

    public static boolean isValid(byte[] rawBody, String signatureHeader, String appSecret) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX) || appSecret == null) {
            return false;
        }
        byte[] expected = sign(rawBody, appSecret);
        byte[] received;
        try {
            received = HexFormat.of().parseHex(signatureHeader.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, received);
    }

    public static String signatureHeader(byte[] rawBody, String appSecret) {
        return PREFIX + HexFormat.of().formatHex(sign(rawBody, appSecret));
    }

    private static byte[] sign(byte[] rawBody, String appSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(rawBody);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no disponible", e);
        }
    }
}
