package com.darmoz.whatsapp.bot;

import com.darmoz.whatsapp.common.BadRequestException;
import com.darmoz.whatsapp.common.NotFoundException;
import com.darmoz.whatsapp.flow.FlowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class BotService {

    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 60;

    private final BotRepository botRepository;
    private final FlowRepository flowRepository;

    BotService(BotRepository botRepository, FlowRepository flowRepository) {
        this.botRepository = botRepository;
        this.flowRepository = flowRepository;
    }

    @Transactional
    public BotResponse create(BotRequest request) {
        if (request.entryFlowId() != null) {
            throw new BadRequestException("El flujo de entrada se elige despues de crear el bot y sus flujos");
        }
        Bot bot = new Bot();
        apply(bot, request);
        return BotResponse.from(botRepository.save(bot));
    }

    @Transactional
    public BotResponse update(UUID id, BotRequest request) {
        Bot bot = findOrThrow(id);
        if (request.entryFlowId() != null && !flowRepository.existsByIdAndBotId(request.entryFlowId(), id)) {
            throw new BadRequestException("El flujo de entrada tiene que ser de este bot");
        }
        apply(bot, request);
        return BotResponse.from(botRepository.save(bot));
    }

    @Transactional(readOnly = true)
    public BotResponse getById(UUID id) {
        return BotResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<BotResponse> list(UUID applicationId) {
        List<Bot> bots = applicationId == null
                ? botRepository.findAllByOrderByNameAsc()
                : botRepository.findByApplicationIdOrderByNameAsc(applicationId);
        return bots.stream().map(BotResponse::from).toList();
    }

    @Transactional
    public void delete(UUID id) {
        botRepository.delete(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Bot findOrThrow(UUID id) {
        return botRepository.findById(id).orElseThrow(() -> new NotFoundException("Bot", id));
    }

    private void apply(Bot bot, BotRequest request) {
        validateBusinessHours(request.businessHours());
        bot.setName(request.name().trim());
        bot.setApplicationId(request.applicationId());
        bot.setActive(request.active() == null || request.active());
        bot.setEntryFlowId(request.entryFlowId());
        bot.setFallbackMessage(request.fallbackMessage());
        bot.setSessionTimeoutMinutes(request.sessionTimeoutMinutes() == null
                ? DEFAULT_SESSION_TIMEOUT_MINUTES : request.sessionTimeoutMinutes());
        bot.setBusinessHours(request.businessHours());
        bot.setOutOfHoursMessage(request.outOfHoursMessage());
        bot.setStaticVariables(request.staticVariables());
    }

    private static void validateBusinessHours(BusinessHours hours) {
        if (hours == null) {
            return;
        }
        try {
            if (hours.timezone() != null) {
                ZoneId.of(hours.timezone());
            }
            if (hours.days() != null) {
                hours.days().values().stream().flatMap(List::stream).forEach(range -> {
                    if (!LocalTime.parse(range.from()).isBefore(LocalTime.parse(range.to()))) {
                        throw new BadRequestException("Rango horario invalido: " + range.from() + "-" + range.to());
                    }
                });
            }
        } catch (DateTimeException | NullPointerException e) {
            throw new BadRequestException("Horario de atencion invalido: " + e.getMessage());
        }
    }
}
