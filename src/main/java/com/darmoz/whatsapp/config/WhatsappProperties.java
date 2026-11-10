package com.darmoz.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "darmoz.whatsapp")
public record WhatsappProperties(
        String secretsKey,
        String publicBaseUrl,
        String graphApiBaseUrl,
        String defaultGraphApiVersion,
        Engine engine
) {

    public record Engine(int maxStepsPerMessage, int httpMaxResponseBytes) {
    }
}
