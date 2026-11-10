package com.darmoz.whatsapp.common.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final SecretCipher cipher = new SecretCipher(KEY);

    @Test
    void encryptsAndDecrypts() {
        String encrypted = cipher.encrypt("EAAGsecret-token");

        assertThat(encrypted).doesNotContain("EAAGsecret-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("EAAGsecret-token");
    }

    @Test
    void usesARandomIvPerValue() {
        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
    }

    @Test
    void failsWithAnotherKey() {
        String encrypted = cipher.encrypt("value");
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        SecretCipher other = new SecretCipher(Base64.getEncoder().encodeToString(otherKey));

        assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingOrShortKey() {
        assertThatThrownBy(() -> new SecretCipher("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new SecretCipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void masksSecrets() {
        assertThat(SecretCipher.mask("EAAGabcdefghx9Q2")).isEqualTo("EAAG…x9Q2");
        assertThat(SecretCipher.mask("short")).isEqualTo("…rt");
        assertThat(SecretCipher.mask(null)).isNull();
    }
}
