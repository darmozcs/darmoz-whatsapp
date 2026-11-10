package com.darmoz.whatsapp;

import com.darmoz.whatsapp.account.MetaAccountCreateRequest;
import com.darmoz.whatsapp.account.MetaAccountResponse;
import com.darmoz.whatsapp.account.MetaAccountService;
import com.darmoz.whatsapp.bot.BotRequest;
import com.darmoz.whatsapp.bot.BotResponse;
import com.darmoz.whatsapp.bot.BotService;
import com.darmoz.whatsapp.conversation.ConversationRepository;
import com.darmoz.whatsapp.conversation.ConversationStatus;
import com.darmoz.whatsapp.flow.FlowDefinition;
import com.darmoz.whatsapp.flow.FlowDtos;
import com.darmoz.whatsapp.flow.FlowService;
import com.darmoz.whatsapp.flow.FlowValidationException;
import com.darmoz.whatsapp.meta.MetaPhoneNumber;
import com.darmoz.whatsapp.meta.WhatsAppCloudClient;
import com.darmoz.whatsapp.messagelog.MessageDirection;
import com.darmoz.whatsapp.messagelog.MessageLogRepository;
import com.darmoz.whatsapp.phonenumber.PhoneNumberResponse;
import com.darmoz.whatsapp.phonenumber.PhoneNumberService;
import com.darmoz.whatsapp.phonenumber.PhoneNumberUpdateRequest;
import com.darmoz.whatsapp.security.DarmozTokenVerifier;
import com.darmoz.whatsapp.webhook.WebhookProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Punta a punta contra un Postgres real (Flyway, JSONB, inserts nativos):
 * alta de cuenta/bot/flujo, publicacion y un webhook entrante que recorre el
 * flujo. Meta esta mockeado. Solo corre con WA_IT_DB_HOST definido, contra
 * una base descartable (nunca la compartida).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "WA_IT_DB_HOST", matches = ".+")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://${WA_IT_DB_HOST}:${WA_IT_DB_PORT:5432}/postgres",
        "spring.datasource.username=postgres",
        "spring.datasource.password=${WA_IT_DB_PASSWORD:postgres}",
        "darmoz.whatsapp.secrets-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
})
class WhatsappFlowIT {

    private static final String CONTACT = "59899123456";

    @MockitoBean
    DarmozTokenVerifier tokenVerifier;

    @MockitoBean
    WhatsAppCloudClient cloudClient;

    @Autowired
    MetaAccountService accountService;
    @Autowired
    PhoneNumberService phoneNumberService;
    @Autowired
    BotService botService;
    @Autowired
    FlowService flowService;
    @Autowired
    WebhookProcessor webhookProcessor;
    @Autowired
    ConversationRepository conversationRepository;
    @Autowired
    MessageLogRepository messageLogRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void inboundMessagesRunThePublishedFlow() {
        String metaPhoneId = "PN-" + UUID.randomUUID();
        when(cloudClient.getPhoneNumbers(any())).thenReturn(List.of(new MetaPhoneNumber(metaPhoneId, "+598 99 000 000", "Darmoz")));
        when(cloudClient.sendMessage(any(), anyString(), anyString(), any())).thenAnswer(inv -> "wamid.out." + UUID.randomUUID());

        MetaAccountResponse account = accountService.create(new MetaAccountCreateRequest(
                "Cuenta IT", "123", "app-secret", "EAAGtoken1234", "WABA1", null, true));
        assertThat(account.accessTokenMasked()).isEqualTo("EAAG…1234");
        String stored = jdbcTemplate.queryForObject("select access_token_enc from wa_meta_account where id = ?",
                String.class, account.id());
        assertThat(stored).doesNotContain("EAAG");

        PhoneNumberResponse phone = accountService.syncPhoneNumbers(account.id()).getFirst();
        BotResponse bot = botService.create(new BotRequest("Bot IT", UUID.randomUUID(), true, null,
                "No entendi", null, null, null, Map.of("storeId", "t1")));
        phoneNumberService.update(phone.id(), new PhoneNumberUpdateRequest(bot.id(), null, null));

        FlowDtos.Detail flow = flowService.create(bot.id(), new FlowDtos.CreateRequest("Principal", null));
        assertThatThrownBy(() -> flowService.publish(bot.id(), flow.id())).isInstanceOf(FlowValidationException.class);

        flowService.saveDraft(bot.id(), flow.id(), new FlowDtos.DraftRequest(new FlowDefinition("q",
                List.of(new FlowDefinition.Node("q", "question", Map.of("x", 0, "y", 0),
                                Map.of("text", "Hola {{contact.name}}, tu email?", "saveAs", "email", "validation", "email")),
                        new FlowDefinition.Node("bye", "end", Map.of("x", 0, "y", 100),
                                Map.of("text", "Gracias, te escribimos a {{email}} ({{bot.storeId}})"))),
                List.of(new FlowDefinition.Edge("e1", "q", "next", "bye")))));
        flowService.publish(bot.id(), flow.id());
        botService.update(bot.id(), new BotRequest("Bot IT", bot.applicationId(), true, flow.id(),
                "No entendi", null, null, null, Map.of("storeId", "t1")));

        webhookProcessor.process(account.id(), webhook(metaPhoneId, "wamid.in.1", "hola"));
        webhookProcessor.process(account.id(), webhook(metaPhoneId, "wamid.in.1", "hola"));
        var conversation = conversationRepository.findAll().stream()
                .filter(c -> c.getPhoneNumberId().equals(phone.id())).findFirst().orElseThrow();
        assertThat(conversation.getCurrentNodeId()).isEqualTo("q");
        assertThat(conversation.getContactName()).isEqualTo("Ana");

        webhookProcessor.process(account.id(), webhook(metaPhoneId, "wamid.in.2", "ana@mail.com"));
        conversation = conversationRepository.findById(conversation.getId()).orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        assertThat(conversation.getVariables()).containsEntry("email", "ana@mail.com");

        var logs = messageLogRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        assertThat(logs).extracting(log -> log.getDirection())
                .containsExactly(MessageDirection.IN, MessageDirection.OUT, MessageDirection.IN, MessageDirection.OUT);
        assertThat(logs.get(3).getPayload().toString()).contains("Gracias, te escribimos a ana@mail.com (t1)");
    }

    private static byte[] webhook(String metaPhoneId, String wamid, String text) {
        return ("""
                {"object":"whatsapp_business_account","entry":[{"id":"WABA1","changes":[{"field":"messages","value":{
                  "messaging_product":"whatsapp",
                  "metadata":{"display_phone_number":"59899000000","phone_number_id":"%s"},
                  "contacts":[{"profile":{"name":"Ana"},"wa_id":"%s"}],
                  "messages":[{"from":"%s","id":"%s","timestamp":"1770000000","type":"text","text":{"body":"%s"}}]
                }}]}]}
                """.formatted(metaPhoneId, CONTACT, CONTACT, wamid, text)).getBytes(StandardCharsets.UTF_8);
    }
}
