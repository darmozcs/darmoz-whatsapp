package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.bot.Bot;
import com.darmoz.whatsapp.common.ExternalApiException;
import com.darmoz.whatsapp.config.WhatsappProperties;
import com.darmoz.whatsapp.conversation.Conversation;
import com.darmoz.whatsapp.conversation.ConversationStatus;
import com.darmoz.whatsapp.flow.Flow;
import com.darmoz.whatsapp.flow.FlowDefinition;
import com.darmoz.whatsapp.flow.FlowRepository;
import com.darmoz.whatsapp.flow.NodeData;
import com.darmoz.whatsapp.flow.NodeType;
import com.darmoz.whatsapp.messagelog.OutboundMessageService;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Avanza la conversacion con un mensaje entrante: aplica la respuesta al nodo
 * de espera actual y ejecuta nodos hasta el proximo que espere respuesta (o
 * hasta que el flujo termine). Lo llama WebhookProcessor dentro de una
 * transaccion con la conversacion bloqueada.
 */
@Component
public class ConversationEngine {

    private static final Logger log = LoggerFactory.getLogger(ConversationEngine.class);
    static final String VARIABLE_LAST_INPUT = "lastInput";

    private final FlowRepository flowRepository;
    private final OutboundMessageService outboundMessageService;
    private final HttpNodeExecutor httpNodeExecutor;
    private final int maxSteps;

    public ConversationEngine(FlowRepository flowRepository, OutboundMessageService outboundMessageService,
                              HttpNodeExecutor httpNodeExecutor, WhatsappProperties properties) {
        this.flowRepository = flowRepository;
        this.outboundMessageService = outboundMessageService;
        this.httpNodeExecutor = httpNodeExecutor;
        this.maxSteps = properties.engine().maxStepsPerMessage();
    }

    public void handle(Bot bot, PhoneNumber phoneNumber, Conversation conversation, InboundMessage message) {
        Instant now = Instant.now();
        if (isExpired(conversation, bot, now)) {
            conversation.reset();
        }
        conversation.setLastInboundAt(now);
        if (message.contactName() != null) {
            conversation.setContactName(message.contactName());
        }
        if (conversation.getStatus() == ConversationStatus.HANDOFF) {
            return;
        }
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            conversation.reset();
        }

        Run run = new Run(bot, phoneNumber, conversation);
        run.variables.put(VARIABLE_LAST_INPUT, message.text() == null ? "" : message.text());
        try {
            if (bot.getBusinessHours() != null && !bot.getBusinessHours().isOpen(now)) {
                run.sendText(bot.getOutOfHoursMessage());
                return;
            }
            if (conversation.getCurrentNodeId() == null) {
                startEntryFlow(run);
            } else {
                resume(run, message);
            }
        } catch (ExternalApiException e) {
            // Meta rechazo un envio: el estado queda donde llego (el error ya esta en wa_message_log).
            log.warn("Conversacion {}: se corta la ejecucion porque fallo un envio", conversation.getId());
        } finally {
            conversation.setVariables(run.variables);
        }
    }

    private void startEntryFlow(Run run) {
        Optional<Flow> flow = Optional.ofNullable(run.bot.getEntryFlowId()).flatMap(flowRepository::findById)
                .filter(candidate -> candidate.getPublishedDefinition() != null);
        if (flow.isEmpty()) {
            run.sendText(run.bot.getFallbackMessage());
            run.conversation.close();
            return;
        }
        FlowDefinition definition = flow.get().getPublishedDefinition();
        execute(run, flow.get().getId(), definition, definition.startNodeId());
    }

    private void resume(Run run, InboundMessage message) {
        Conversation conversation = run.conversation;
        Optional<FlowDefinition> definition = Optional.ofNullable(conversation.getCurrentFlowId())
                .flatMap(flowRepository::findById)
                .map(Flow::getPublishedDefinition);
        Optional<FlowDefinition.Node> node = definition.flatMap(d -> d.node(conversation.getCurrentNodeId()));
        Optional<NodeType> type = node.flatMap(n -> NodeType.fromKey(n.type()));
        if (type.isEmpty() || !type.get().waitsForInput()) {
            // El flujo se re-publico sin ese nodo (o se borro): se arranca de nuevo.
            conversation.reset();
            startEntryFlow(run);
            return;
        }

        String handle = InputMatcher.apply(node.get(), type.get(), message, run.variables);
        Optional<String> next = definition.get().target(node.get().id(), handle);
        if (next.isPresent()) {
            execute(run, conversation.getCurrentFlowId(), definition.get(), next.get());
        } else if (NodeType.HANDLE_INVALID.equals(handle)) {
            // Respuesta invalida sin salida "invalid" conectada: se vuelve a preguntar.
            run.sendText(NodeData.string(node.get().safeData(), "errorMessage"));
            execute(run, conversation.getCurrentFlowId(), definition.get(), node.get().id());
        } else {
            conversation.close();
        }
    }

    /** Ejecuta desde nodeId hasta un nodo de espera, handoff, end o una salida sin conectar. */
    private void execute(Run run, UUID flowId, FlowDefinition definition, String nodeId) {
        Conversation conversation = run.conversation;
        UUID currentFlowId = flowId;
        FlowDefinition currentDefinition = definition;
        String currentNodeId = nodeId;

        for (int step = 0; step < maxSteps; step++) {
            Optional<FlowDefinition.Node> node = currentDefinition.node(currentNodeId);
            Optional<NodeType> type = node.flatMap(n -> NodeType.fromKey(n.type()));
            if (type.isEmpty()) {
                conversation.close();
                return;
            }
            Map<String, Object> data = node.get().safeData();
            VariableScope scope = run.scope();
            conversation.setCurrentFlowId(currentFlowId);

            String handle;
            switch (type.get()) {
                case MESSAGE -> {
                    run.send(OutboundPayloads.message(data, scope));
                    handle = NodeType.HANDLE_NEXT;
                }
                case BUTTONS, LIST, QUESTION -> {
                    run.send(switch (type.get()) {
                        case BUTTONS -> OutboundPayloads.buttons(data, scope);
                        case LIST -> OutboundPayloads.list(data, scope);
                        default -> OutboundPayloads.text(TemplateRenderer.render(NodeData.string(data, "text"), scope));
                    });
                    conversation.setCurrentNodeId(currentNodeId);
                    conversation.setStatus(ConversationStatus.ACTIVE);
                    return;
                }
                case CONDITION -> handle = ConditionEvaluator.evaluate(data, scope);
                case HTTP -> handle = httpNodeExecutor.execute(data, run.bot.getApplicationId(), run.phoneNumber,
                        scope, run.variables);
                case SET_VARIABLE -> {
                    run.variables.put(NodeData.string(data, "variable"),
                            TemplateRenderer.render(NodeData.string(data, "value"), scope));
                    handle = NodeType.HANDLE_NEXT;
                }
                case GOTO_FLOW -> {
                    Optional<Flow> target = parseUuid(NodeData.string(data, "flowId"))
                            .flatMap(id -> flowRepository.findByIdAndBotId(id, run.bot.getId()))
                            .filter(flow -> flow.getPublishedDefinition() != null);
                    if (target.isEmpty()) {
                        conversation.close();
                        return;
                    }
                    currentFlowId = target.get().getId();
                    currentDefinition = target.get().getPublishedDefinition();
                    currentNodeId = currentDefinition.startNodeId();
                    continue;
                }
                case HANDOFF -> {
                    run.sendText(TemplateRenderer.render(NodeData.string(data, "text"), scope));
                    conversation.setCurrentNodeId(null);
                    conversation.setStatus(ConversationStatus.HANDOFF);
                    return;
                }
                case END -> {
                    run.sendText(TemplateRenderer.render(NodeData.string(data, "text"), scope));
                    conversation.close();
                    return;
                }
                default -> throw new IllegalStateException("Tipo sin implementar: " + type.get());
            }

            Optional<String> next = currentDefinition.target(currentNodeId, handle);
            if (next.isEmpty()) {
                conversation.close();
                return;
            }
            currentNodeId = next.get();
        }
        log.warn("Conversacion {}: se corto despues de {} pasos (loop en el flujo {}?)",
                conversation.getId(), maxSteps, currentFlowId);
        conversation.close();
    }

    private static boolean isExpired(Conversation conversation, Bot bot, Instant now) {
        return conversation.getLastInboundAt() != null
                && Duration.between(conversation.getLastInboundAt(), now).toMinutes() >= bot.getSessionTimeoutMinutes();
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.ofNullable(value).map(UUID::fromString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Estado de una ejecucion: las variables se modifican sobre una copia y se guardan al final. */
    private final class Run {

        private final Bot bot;
        private final PhoneNumber phoneNumber;
        private final Conversation conversation;
        private final Map<String, Object> variables;

        Run(Bot bot, PhoneNumber phoneNumber, Conversation conversation) {
            this.bot = bot;
            this.phoneNumber = phoneNumber;
            this.conversation = conversation;
            this.variables = new HashMap<>(conversation.getVariables());
        }

        VariableScope scope() {
            return new VariableScope(conversation.getContactWaId(), conversation.getContactName(),
                    bot.getStaticVariables() == null ? Map.of() : bot.getStaticVariables(), variables);
        }

        void send(Map<String, Object> payload) {
            outboundMessageService.send(phoneNumber, conversation.getContactWaId(), conversation.getId(), payload);
        }

        void sendText(String text) {
            if (text != null && !text.isBlank()) {
                send(OutboundPayloads.text(TemplateRenderer.render(text, scope())));
            }
        }
    }
}
