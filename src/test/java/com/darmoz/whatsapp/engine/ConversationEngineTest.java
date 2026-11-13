package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.account.MetaAccount;
import com.darmoz.whatsapp.bot.Bot;
import com.darmoz.whatsapp.config.WhatsappProperties;
import com.darmoz.whatsapp.conversation.Conversation;
import com.darmoz.whatsapp.conversation.ConversationStatus;
import com.darmoz.whatsapp.flow.Flow;
import com.darmoz.whatsapp.flow.FlowDefinition;
import com.darmoz.whatsapp.flow.FlowRepository;
import com.darmoz.whatsapp.flow.NodeType;
import com.darmoz.whatsapp.messagelog.OutboundMessageService;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Flujo "consultar saldo": botones -> http -> mensaje con el total / end si falla. */
class ConversationEngineTest {

    private static final UUID FLOW_ID = UUID.randomUUID();
    private static final String CONTACT = "59899123456";

    private final FlowRepository flowRepository = mock(FlowRepository.class);
    private final OutboundMessageService outbound = mock(OutboundMessageService.class);
    private final HttpNodeExecutor httpNodeExecutor = mock(HttpNodeExecutor.class);
    private final ConversationEngine engine = new ConversationEngine(flowRepository, outbound, httpNodeExecutor,
            new WhatsappProperties("k", "https://x", "https://graph", "v21.0", new WhatsappProperties.Engine(50, 1000)));

    private Bot bot;
    private PhoneNumber phoneNumber;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        FlowDefinition definition = new FlowDefinition("menu",
                List.of(
                        new FlowDefinition.Node("menu", "buttons", null, Map.of("text", "Hola {{contact.name}}",
                                "buttons", List.of(Map.of("id", "saldo", "title", "Mi saldo")))),
                        new FlowDefinition.Node("api", "http", null, Map.of("method", "GET", "path", "/saldo",
                                "saveAs", "api")),
                        new FlowDefinition.Node("ok", "message", null, Map.of("text", "Tu total es ${{api.total}}")),
                        new FlowDefinition.Node("ko", "end", null, Map.of("text", "No encontramos tu pedido"))),
                List.of(new FlowDefinition.Edge("e1", "menu", "saldo", "api"),
                        new FlowDefinition.Edge("e2", "api", "success", "ok"),
                        new FlowDefinition.Edge("e3", "api", "error", "ko")));
        Flow flow = new Flow(UUID.randomUUID());
        ReflectionTestUtils.setField(flow, "id", FLOW_ID);
        flow.setDraftDefinition(definition);
        flow.publish();
        when(flowRepository.findById(FLOW_ID)).thenReturn(Optional.of(flow));

        bot = new Bot();
        bot.setApplicationId(UUID.randomUUID());
        bot.setEntryFlowId(FLOW_ID);
        phoneNumber = new PhoneNumber(new MetaAccount(), "123");
        conversation = new Conversation(UUID.randomUUID(), UUID.randomUUID(), CONTACT);
    }

    private static InboundMessage text(String body) {
        return new InboundMessage(UUID.randomUUID().toString(), CONTACT, "Ana", "text", body, null, Instant.now(), Map.of());
    }

    private static InboundMessage buttonReply(String id) {
        return new InboundMessage(UUID.randomUUID().toString(), CONTACT, "Ana", "interactive", "Mi saldo", id,
                Instant.now(), Map.of());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sentPayloads(int count) {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(outbound, times(count)).send(eq(phoneNumber), eq(CONTACT), eq(conversation.getId()), captor.capture());
        return captor.getAllValues();
    }

    @Test
    void firstMessageSendsTheMenuAndWaits() {
        engine.handle(bot, phoneNumber, conversation, text("hola"));

        Map<String, Object> payload = sentPayloads(1).getFirst();
        assertThat(payload.get("type")).isEqualTo("interactive");
        assertThat(conversation.getCurrentNodeId()).isEqualTo("menu");
        assertThat(conversation.getCurrentFlowId()).isEqualTo(FLOW_ID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buttonReplyCallsTheApiAndAnswersWithTheTotal() {
        engine.handle(bot, phoneNumber, conversation, text("hola"));
        when(httpNodeExecutor.execute(any(), eq(bot.getApplicationId()), any(), any(), any())).thenAnswer(invocation -> {
            Map<String, Object> variables = invocation.getArgument(4);
            variables.put("api", Map.of("total", 1500));
            return NodeType.HANDLE_SUCCESS;
        });

        engine.handle(bot, phoneNumber, conversation, buttonReply("saldo"));

        Map<String, Object> answer = sentPayloads(2).get(1);
        assertThat(((Map<String, Object>) answer.get("text")).get("body")).isEqualTo("Tu total es $1500");
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        assertThat(conversation.getCurrentNodeId()).isNull();
    }

    @Test
    void apiErrorGoesToTheErrorBranch() {
        engine.handle(bot, phoneNumber, conversation, text("hola"));
        when(httpNodeExecutor.execute(any(), any(), any(), any(), any())).thenReturn(NodeType.HANDLE_ERROR);

        engine.handle(bot, phoneNumber, conversation, text("1"));

        assertThat(sentPayloads(2).get(1).toString()).contains("No encontramos tu pedido");
    }

    @Test
    void invalidAnswerRepeatsTheMenu() {
        engine.handle(bot, phoneNumber, conversation, text("hola"));

        engine.handle(bot, phoneNumber, conversation, text("cualquier cosa"));

        assertThat(sentPayloads(2).get(1).get("type")).isEqualTo("interactive");
        assertThat(conversation.getCurrentNodeId()).isEqualTo("menu");
        verify(httpNodeExecutor, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void handoffConversationsAreIgnored() {
        conversation.setStatus(ConversationStatus.HANDOFF);
        conversation.setLastInboundAt(Instant.now());

        engine.handle(bot, phoneNumber, conversation, text("hola"));

        verify(outbound, never()).send(any(), any(), any(), any());
    }

    @Test
    void withoutPublishedEntryFlowSendsTheFallback() {
        bot.setEntryFlowId(null);
        bot.setFallbackMessage("Estamos configurando el bot");

        engine.handle(bot, phoneNumber, conversation, text("hola"));

        assertThat(sentPayloads(1).getFirst().toString()).contains("Estamos configurando el bot");
    }
}
