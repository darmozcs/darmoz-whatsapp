package com.darmoz.whatsapp.account;

/** Lo que hay que pegar en Meta (App > WhatsApp > Configuracion > Webhook). */
public record WebhookInfoResponse(String callbackUrl, String verifyToken, String subscribeFields) {
}
