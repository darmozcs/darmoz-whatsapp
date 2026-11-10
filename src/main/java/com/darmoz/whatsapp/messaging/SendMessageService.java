package com.darmoz.whatsapp.messaging;

import com.darmoz.whatsapp.bot.Bot;
import com.darmoz.whatsapp.bot.BotRepository;
import com.darmoz.whatsapp.common.BadRequestException;
import com.darmoz.whatsapp.common.UnknownClientException;
import com.darmoz.whatsapp.conversation.Conversation;
import com.darmoz.whatsapp.conversation.ConversationRepository;
import com.darmoz.whatsapp.integration.AppIntegrationRepository;
import com.darmoz.whatsapp.messagelog.MessageLogResponse;
import com.darmoz.whatsapp.messagelog.OutboundMessageService;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import com.darmoz.whatsapp.phonenumber.PhoneNumberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Envio de mensajes pedido por otro servicio (POST /messages).
 * El X-Client-Id es el applicationId de darmoz-auth y tiene que tener una integracion
 * registrada. El mensaje sale por un numero activo asignado a un bot de esa aplicacion.
 * Fuera de la ventana de 24 h Meta rechaza el texto libre (solo templates): vuelve 502.
 */
@Service
public class SendMessageService {

    private final AppIntegrationRepository integrationRepository;
    private final BotRepository botRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final ConversationRepository conversationRepository;
    private final OutboundMessageService outboundMessageService;

    SendMessageService(AppIntegrationRepository integrationRepository, BotRepository botRepository,
                       PhoneNumberRepository phoneNumberRepository, ConversationRepository conversationRepository,
                       OutboundMessageService outboundMessageService) {
        this.integrationRepository = integrationRepository;
        this.botRepository = botRepository;
        this.phoneNumberRepository = phoneNumberRepository;
        this.conversationRepository = conversationRepository;
        this.outboundMessageService = outboundMessageService;
    }

    @Transactional
    public MessageLogResponse send(String clientIdHeader, SendMessageRequest request) {
        UUID applicationId = resolveApplication(clientIdHeader);
        PhoneNumber from = resolveSender(applicationId, request.phoneNumberId());
        UUID conversationId = conversationRepository
                .findByPhoneNumberIdAndContactWaId(from.getId(), request.to())
                .map(Conversation::getId)
                .orElse(null);
        return MessageLogResponse.from(outboundMessageService.send(from, request.to(), conversationId,
                OutboundMessageService.text(request.text())));
    }

    private UUID resolveApplication(String clientIdHeader) {
        UUID applicationId;
        try {
            applicationId = UUID.fromString(clientIdHeader.trim());
        } catch (IllegalArgumentException ex) {
            throw new UnknownClientException(clientIdHeader);
        }
        if (!integrationRepository.existsByApplicationId(applicationId)) {
            throw new UnknownClientException(clientIdHeader);
        }
        return applicationId;
    }

    private PhoneNumber resolveSender(UUID applicationId, UUID requestedId) {
        List<UUID> botIds = botRepository.findByApplicationIdOrderByNameAsc(applicationId).stream()
                .map(Bot::getId)
                .toList();
        List<PhoneNumber> candidates = botIds.isEmpty()
                ? List.of()
                : phoneNumberRepository.findByBotIdInAndActiveTrue(botIds);

        if (requestedId != null) {
            return candidates.stream()
                    .filter(phone -> phone.getId().equals(requestedId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "El numero " + requestedId + " no esta activo o no pertenece a un bot de la aplicacion"));
        }
        if (candidates.isEmpty()) {
            throw new BadRequestException("La aplicacion no tiene numeros activos asignados a un bot");
        }
        if (candidates.size() > 1) {
            throw new BadRequestException("La aplicacion tiene varios numeros activos: indicar phoneNumberId");
        }
        return candidates.getFirst();
    }
}
