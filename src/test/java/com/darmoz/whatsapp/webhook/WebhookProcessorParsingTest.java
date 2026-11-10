package com.darmoz.whatsapp.webhook;

import com.darmoz.whatsapp.engine.InboundMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookProcessorParsingTest {

    private static final List<WebhookPayload.Contact> CONTACTS =
            List.of(new WebhookPayload.Contact(new WebhookPayload.Profile("Ana"), "59899123456"));

    @Test
    void parsesTextMessages() {
        InboundMessage message = InboundMessageParser.parse(Map.of(
                "from", "59899123456", "id", "wamid.1", "timestamp", "1770000000", "type", "text",
                "text", Map.of("body", "hola")), CONTACTS);

        assertThat(message.text()).isEqualTo("hola");
        assertThat(message.replyId()).isNull();
        assertThat(message.contactName()).isEqualTo("Ana");
        assertThat(message.timestamp().getEpochSecond()).isEqualTo(1770000000L);
    }

    @Test
    void parsesButtonAndListReplies() {
        InboundMessage button = InboundMessageParser.parse(Map.of(
                "from", "59899123456", "id", "wamid.2", "type", "interactive",
                "interactive", Map.of("type", "button_reply", "button_reply", Map.of("id", "saldo", "title", "Mi saldo"))),
                CONTACTS);
        InboundMessage list = InboundMessageParser.parse(Map.of(
                "from", "59899123456", "id", "wamid.3", "type", "interactive",
                "interactive", Map.of("type", "list_reply", "list_reply", Map.of("id", "row-1", "title", "Fila"))),
                CONTACTS);

        assertThat(button.replyId()).isEqualTo("saldo");
        assertThat(button.text()).isEqualTo("Mi saldo");
        assertThat(list.replyId()).isEqualTo("row-1");
    }

    @Test
    void unsupportedTypesHaveNoText() {
        InboundMessage image = InboundMessageParser.parse(Map.of(
                "from", "59899123456", "id", "wamid.4", "type", "image", "image", Map.of("id", "media")), List.of());

        assertThat(image.text()).isNull();
        assertThat(image.contactName()).isNull();
    }
}
