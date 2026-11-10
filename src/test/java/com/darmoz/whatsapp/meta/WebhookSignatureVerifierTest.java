package com.darmoz.whatsapp.meta;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    private static final byte[] BODY = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void acceptsTheSignatureOfTheBody() {
        String header = WebhookSignatureVerifier.signatureHeader(BODY, "app-secret");

        assertThat(header).startsWith("sha256=");
        assertThat(WebhookSignatureVerifier.isValid(BODY, header, "app-secret")).isTrue();
    }

    @Test
    void rejectsAnotherSecretOrBody() {
        String header = WebhookSignatureVerifier.signatureHeader(BODY, "app-secret");

        assertThat(WebhookSignatureVerifier.isValid(BODY, header, "other")).isFalse();
        assertThat(WebhookSignatureVerifier.isValid("{}".getBytes(StandardCharsets.UTF_8), header, "app-secret")).isFalse();
    }

    @Test
    void rejectsMissingOrMalformedHeader() {
        assertThat(WebhookSignatureVerifier.isValid(BODY, null, "app-secret")).isFalse();
        assertThat(WebhookSignatureVerifier.isValid(BODY, "sha1=abc", "app-secret")).isFalse();
        assertThat(WebhookSignatureVerifier.isValid(BODY, "sha256=zz", "app-secret")).isFalse();
    }
}
