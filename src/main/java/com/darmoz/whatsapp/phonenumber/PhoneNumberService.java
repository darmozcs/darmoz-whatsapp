package com.darmoz.whatsapp.phonenumber;

import com.darmoz.whatsapp.bot.BotRepository;
import com.darmoz.whatsapp.common.NotFoundException;
import com.darmoz.whatsapp.messagelog.MessageLogResponse;
import com.darmoz.whatsapp.messagelog.OutboundMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PhoneNumberService {

    private final PhoneNumberRepository phoneNumberRepository;
    private final BotRepository botRepository;
    private final OutboundMessageService outboundMessageService;

    PhoneNumberService(PhoneNumberRepository phoneNumberRepository, BotRepository botRepository,
                       OutboundMessageService outboundMessageService) {
        this.phoneNumberRepository = phoneNumberRepository;
        this.botRepository = botRepository;
        this.outboundMessageService = outboundMessageService;
    }

    @Transactional(readOnly = true)
    public List<PhoneNumberResponse> list(UUID accountId, UUID botId) {
        List<PhoneNumber> phoneNumbers;
        if (accountId != null) {
            phoneNumbers = phoneNumberRepository.findByAccountId(accountId);
        } else if (botId != null) {
            phoneNumbers = phoneNumberRepository.findByBotId(botId);
        } else {
            phoneNumbers = phoneNumberRepository.findAllByOrderByCreatedAtAsc();
        }
        return phoneNumbers.stream().map(PhoneNumberResponse::from).toList();
    }

    @Transactional
    public PhoneNumberResponse update(UUID id, PhoneNumberUpdateRequest request) {
        PhoneNumber phoneNumber = findOrThrow(id);
        if (Boolean.TRUE.equals(request.unassignBot())) {
            phoneNumber.setBotId(null);
        } else if (request.botId() != null) {
            if (!botRepository.existsById(request.botId())) {
                throw new NotFoundException("Bot", request.botId());
            }
            phoneNumber.setBotId(request.botId());
        }
        if (request.active() != null) {
            phoneNumber.setActive(request.active());
        }
        return PhoneNumberResponse.from(phoneNumberRepository.save(phoneNumber));
    }

    @Transactional
    public void delete(UUID id) {
        phoneNumberRepository.delete(findOrThrow(id));
    }

    /** Fuera de la ventana de 24 h Meta lo rechaza (solo templates): el error vuelve como 502. */
    @Transactional
    public MessageLogResponse sendTestMessage(UUID id, TestMessageRequest request) {
        PhoneNumber phoneNumber = findOrThrow(id);
        return MessageLogResponse.from(outboundMessageService.send(phoneNumber, request.to(), null,
                OutboundMessageService.text(request.text())));
    }

    private PhoneNumber findOrThrow(UUID id) {
        return phoneNumberRepository.findWithAccountById(id)
                .orElseThrow(() -> new NotFoundException("Numero", id));
    }
}
