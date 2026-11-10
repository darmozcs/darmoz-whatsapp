package com.darmoz.whatsapp.messagelog;

import com.darmoz.whatsapp.common.ExternalApiException;
import com.darmoz.whatsapp.meta.WhatsAppCloudClient;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Manda un mensaje por la Cloud API y deja la fila OUT en wa_message_log
 * (SENT con su wamid, o FAILED con el error). Los statuses posteriores
 * (DELIVERED, READ) llegan por el webhook.
 */
@Service
public class OutboundMessageService {

    private static final Logger log = LoggerFactory.getLogger(OutboundMessageService.class);

    private final WhatsAppCloudClient cloudClient;
    private final MessageLogRepository messageLogRepository;

    public OutboundMessageService(WhatsAppCloudClient cloudClient, MessageLogRepository messageLogRepository) {
        this.cloudClient = cloudClient;
        this.messageLogRepository = messageLogRepository;
    }

    /** @throws ExternalApiException si Meta rechaza el mensaje (igual queda registrado como FAILED) */
    public MessageLog send(PhoneNumber phoneNumber, String to, UUID conversationId, Map<String, Object> payload) {
        MessageLog entry = new MessageLog();
        entry.setConversationId(conversationId);
        entry.setPhoneNumberId(phoneNumber.getId());
        entry.setDirection(MessageDirection.OUT);
        entry.setContactWaId(to);
        entry.setType(String.valueOf(payload.get("type")));
        entry.setPayload(payload);
        try {
            entry.setWaMessageId(cloudClient.sendMessage(phoneNumber.getAccount(), phoneNumber.getPhoneNumberId(), to, payload));
            entry.setStatus(MessageStatus.SENT);
            return messageLogRepository.save(entry);
        } catch (ExternalApiException e) {
            log.warn("No se pudo enviar el mensaje a {} desde {}: {}", to, phoneNumber.getPhoneNumberId(), e.getMessage());
            entry.setStatus(MessageStatus.FAILED);
            entry.setError(e.getMessage());
            messageLogRepository.save(entry);
            throw e;
        }
    }

    public static Map<String, Object> text(String body) {
        return Map.of("type", "text", "text", Map.of("body", body, "preview_url", false));
    }
}
