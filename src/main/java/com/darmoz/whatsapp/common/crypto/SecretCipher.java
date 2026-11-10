package com.darmoz.whatsapp.common.crypto;

import com.darmoz.whatsapp.config.WhatsappProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra los secretos que se cargan desde el dashboard (tokens de Meta, API
 * keys de las aplicaciones) con AES-256-GCM. Cada valor lleva su IV:
 * base64(iv || ciphertext+tag). La llave vive solo en WA_SECRETS_KEY.
 */
@Component
public class SecretCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_LENGTH = 32;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SecretCipher(WhatsappProperties properties) {
        this(properties.secretsKey());
    }

    SecretCipher(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("Hay que configurar WA_SECRETS_KEY (32 bytes en base64)");
        }
        byte[] raw = Base64.getDecoder().decode(base64Key.trim());
        if (raw.length != KEY_LENGTH) {
            throw new IllegalStateException("WA_SECRETS_KEY tiene que ser de 32 bytes, tiene " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el secreto (WA_SECRETS_KEY cambio?)", e);
        }
    }

    /** "EAAG…x9Q2": lo que la API devuelve en lugar del secreto. */
    public static String mask(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        if (plain.length() <= 8) {
            return "…" + plain.substring(plain.length() - Math.min(2, plain.length()));
        }
        return plain.substring(0, 4) + "…" + plain.substring(plain.length() - 4);
    }
}
