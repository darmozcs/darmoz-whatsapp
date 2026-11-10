package com.darmoz.whatsapp.webhook;

import com.darmoz.whatsapp.bot.Bot;
import com.darmoz.whatsapp.bot.BotRepository;
import com.darmoz.whatsapp.conversation.Conversation;
import com.darmoz.whatsapp.conversation.ConversationRepository;
import com.darmoz.whatsapp.engine.ConversationEngine;
import com.darmoz.whatsapp.engine.InboundMessage;
import com.darmoz.whatsapp.messagelog.MessageLogRepository;
import com.darmoz.whatsapp.messagelog.MessageStatus;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import com.darmoz.whatsapp.phonenumber.PhoneNumberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Procesa el payload del webhook fuera del request. Cada mensaje entrante va
 * en su propia transaccion, con la conversacion del contacto bloqueada.
 */
@Service
public class WebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookProcessor.class);
    private static final String FIELD_MESSAGES = "messages";

    private final JsonMapper jsonMapper;
    private final TransactionTemplate transactionTemplate;
    private final PhoneNumberRepository phoneNumberRepository;
    private final BotRepository botRepository;
    private final ConversationRepository conversationRepository;
    private final MessageLogRepository messageLogRepository;
    private final ConversationEngine conversationEngine;

    WebhookProcessor(JsonMapper jsonMapper, TransactionTemplate transactionTemplate,
                     PhoneNumberRepository phoneNumberRepository, BotRepository botRepository,
                     ConversationRepository conversationRepository, MessageLogRepository messageLogRepository,
                     ConversationEngine conversationEngine) {
        this.jsonMapper = jsonMapper;
        this.transactionTemplate = transactionTemplate;
        this.phoneNumberRepository = phoneNumberRepository;
        this.botRepository = botRepository;
        this.conversationRepository = conversationRepository;
        this.messageLogRepository = messageLogRepository;
        this.conversationEngine = conversationEngine;
    }

    @Async
    public void processAsync(UUID accountId, byte[] body) {
        process(accountId, body);
    }

    public void process(UUID accountId, byte[] body) {
        WebhookPayload payload;
        try {
            payload = jsonMapper.readValue(body, WebhookPayload.class);
        } catch (JacksonException e) {
            log.warn("Webhook de la cuenta {} con JSON invalido: {}", accountId, e.getOriginalMessage());
            return;
        }
        if (payload.entry() == null) {
            return;
        }
        for (WebhookPayload.Entry entry : payload.entry()) {
            if (entry.changes() == null) {
                continue;
            }
            for (WebhookPayload.Change change : entry.changes()) {
                if (FIELD_MESSAGES.equals(change.field()) && change.value() != null) {
                    processValue(accountId, change.value());
                }
            }
        }
    }

    private void processValue(UUID accountId, WebhookPayload.Value value) {
        if (value.statuses() != null) {
            value.statuses().forEach(status -> safely(() -> transactionTemplate.executeWithoutResult(tx -> updateStatus(status))));
        }
        if (value.messages() == null || value.metadata() == null) {
            return;
        }
        String metaPhoneNumberId = value.metadata().phoneNumberId();
        for (Map<String, Object> raw : value.messages()) {
            InboundMessage message = InboundMessageParser.parse(raw, value.contacts());
            if (message.waMessageId() == null || message.from() == null) {
                continue;
            }
            safely(() -> transactionTemplate.executeWithoutResult(tx ->
                    handleMessage(accountId, metaPhoneNumberId, message)));
        }
    }

    private void handleMessage(UUID accountId, String metaPhoneNumberId, InboundMessage message) {
        Optional<PhoneNumber> phoneNumber = phoneNumberRepository.findByPhoneNumberId(metaPhoneNumberId)
                .filter(candidate -> candidate.getAccount().getId().equals(accountId));
        if (phoneNumber.isEmpty()) {
            log.warn("Mensaje para el numero {} que no esta sincronizado en la cuenta {}", metaPhoneNumberId, accountId);
            return;
        }
        PhoneNumber number = phoneNumber.get();
        Optional<Bot> bot = Optional.ofNullable(number.getBotId()).flatMap(botRepository::findById)
                .filter(candidate -> candidate.isActive() && number.isActive());
        if (bot.isEmpty()) {
            // Sin bot: queda registrado pero nadie responde.
            insertInbound(null, number, message);
            return;
        }

        conversationRepository.insertIfAbsent(UUID.randomUUID(), number.getId(), message.from());
        Conversation conversation = conversationRepository.lockByContact(number.getId(), message.from()).orElseThrow();
        if (!insertInbound(conversation.getId(), number, message)) {
            return;
        }
        conversationEngine.handle(bot.get(), number, conversation, message);
    }

    /** false si el mensaje ya se habia procesado (reintento de Meta). */
    private boolean insertInbound(UUID conversationId, PhoneNumber number, InboundMessage message) {
        return messageLogRepository.insertInboundIfAbsent(UUID.randomUUID(), conversationId, number.getId(),
                message.waMessageId(), message.from(), message.type() == null ? "unknown" : message.type(),
                jsonMapper.writeValueAsString(message.raw())) > 0;
    }

    private void updateStatus(WebhookPayload.Status status) {
        if (status.id() == null || status.status() == null) {
            return;
        }
        MessageStatus newStatus;
        try {
            newStatus = MessageStatus.valueOf(status.status().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return;
        }
        messageLogRepository.findByWaMessageId(status.id()).ifPresent(entry -> {
            if (newStatus.isAfter(entry.getStatus())) {
                entry.setStatus(newStatus);
                if (newStatus == MessageStatus.FAILED && status.errors() != null) {
                    entry.setError(jsonMapper.writeValueAsString(status.errors()));
                }
            }
        });
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Error procesando el webhook", e);
        }
    }
}
