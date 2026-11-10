package com.darmoz.whatsapp.flow;

import com.darmoz.whatsapp.bot.BotRepository;
import com.darmoz.whatsapp.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FlowService {

    private final FlowRepository flowRepository;
    private final BotRepository botRepository;
    private final FlowValidator flowValidator;

    FlowService(FlowRepository flowRepository, BotRepository botRepository, FlowValidator flowValidator) {
        this.flowRepository = flowRepository;
        this.botRepository = botRepository;
        this.flowValidator = flowValidator;
    }

    @Transactional
    public FlowDtos.Detail create(UUID botId, FlowDtos.CreateRequest request) {
        if (!botRepository.existsById(botId)) {
            throw new NotFoundException("Bot", botId);
        }
        Flow flow = new Flow(botId);
        flow.setName(request.name().trim());
        flow.setDraftDefinition(request.definition() == null ? FlowDefinition.empty() : request.definition());
        return FlowDtos.Detail.from(flowRepository.save(flow));
    }

    @Transactional(readOnly = true)
    public List<FlowDtos.Summary> list(UUID botId) {
        return flowRepository.findByBotIdOrderByCreatedAtAsc(botId).stream().map(FlowDtos.Summary::from).toList();
    }

    @Transactional(readOnly = true)
    public FlowDtos.Detail get(UUID botId, UUID flowId) {
        return FlowDtos.Detail.from(findOrThrow(botId, flowId));
    }

    @Transactional
    public FlowDtos.Detail rename(UUID botId, UUID flowId, FlowDtos.RenameRequest request) {
        Flow flow = findOrThrow(botId, flowId);
        flow.setName(request.name().trim());
        return FlowDtos.Detail.from(flowRepository.save(flow));
    }

    @Transactional
    public FlowDtos.Detail saveDraft(UUID botId, UUID flowId, FlowDtos.DraftRequest request) {
        Flow flow = findOrThrow(botId, flowId);
        flow.setDraftDefinition(request.definition());
        return FlowDtos.Detail.from(flowRepository.save(flow));
    }

    /** @throws FlowValidationException (422) con los errores por nodo */
    @Transactional
    public FlowDtos.PublishResponse publish(UUID botId, UUID flowId) {
        Flow flow = findOrThrow(botId, flowId);
        List<FlowValidator.Issue> issues = flowValidator.validate(flow.getDraftDefinition(),
                id -> flowRepository.existsByIdAndBotId(id, botId));
        List<FlowValidator.Issue> errors = issues.stream().filter(FlowValidator.Issue::isError).toList();
        if (!errors.isEmpty()) {
            throw new FlowValidationException(errors);
        }
        flow.publish();
        return new FlowDtos.PublishResponse(FlowDtos.Detail.from(flowRepository.save(flow)),
                issues.stream().filter(issue -> !issue.isError()).toList());
    }

    /** Valida sin publicar (para el boton "Revisar" del editor). */
    @Transactional(readOnly = true)
    public List<FlowValidator.Issue> validate(UUID botId, UUID flowId) {
        Flow flow = findOrThrow(botId, flowId);
        return flowValidator.validate(flow.getDraftDefinition(), id -> flowRepository.existsByIdAndBotId(id, botId));
    }

    @Transactional
    public void delete(UUID botId, UUID flowId) {
        flowRepository.delete(findOrThrow(botId, flowId));
    }

    private Flow findOrThrow(UUID botId, UUID flowId) {
        return flowRepository.findByIdAndBotId(flowId, botId).orElseThrow(() -> new NotFoundException("Flujo", flowId));
    }
}
