package com.darmoz.whatsapp.messaging;

import com.darmoz.whatsapp.bot.Bot;
import com.darmoz.whatsapp.bot.BotRepository;
import com.darmoz.whatsapp.common.BadRequestException;
import com.darmoz.whatsapp.common.UnknownClientException;
import com.darmoz.whatsapp.conversation.Conversation;
import com.darmoz.whatsapp.conversation.ConversationRepository;
import com.darmoz.whatsapp.integration.AppIntegrationRepository;
import com.darmoz.whatsapp.messagelog.MessageDirection;
import com.darmoz.whatsapp.messagelog.MessageLog;
import com.darmoz.whatsapp.messagelog.OutboundMessageService;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import com.darmoz.whatsapp.phonenumber.PhoneNumberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendMessageServiceTest {

    private static final UUID APP_ID = UUID.randomUUID();
    private static final UUID BOT_ID = UUID.randomUUID();
    private static final String TO = "59899123456";

    private final AppIntegrationRepository integrationRepository = mock(AppIntegrationRepository.class);
    private final BotRepository botRepository = mock(BotRepository.class);
    private final PhoneNumberRepository phoneNumberRepository = mock(PhoneNumberRepository.class);
    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final OutboundMessageService outbound = mock(OutboundMessageService.class);

    private final SendMessageService service = new SendMessageService(integrationRepository, botRepository,
            phoneNumberRepository, conversationRepository, outbound);

    @BeforeEach
    void setUp() {
        Bot bot = mock(Bot.class);
        when(bot.getId()).thenReturn(BOT_ID);
        when(integrationRepository.existsByApplicationId(APP_ID)).thenReturn(true);
        when(botRepository.findByApplicationIdOrderByNameAsc(APP_ID)).thenReturn(List.of(bot));
        MessageLog sent = new MessageLog();
        sent.setDirection(MessageDirection.OUT);
        when(outbound.send(any(), anyString(), any(), any())).thenReturn(sent);
    }

    @Test
    void sendsFromTheOnlyActiveNumberAndLinksTheConversation() {
        PhoneNumber phone = phone();
        when(phoneNumberRepository.findByBotIdInAndActiveTrue(List.of(BOT_ID))).thenReturn(List.of(phone));
        Conversation conversation = new Conversation(UUID.randomUUID(), phone.getId(), TO);
        when(conversationRepository.findByPhoneNumberIdAndContactWaId(phone.getId(), TO))
                .thenReturn(Optional.of(conversation));

        service.send(APP_ID.toString(), new SendMessageRequest(TO, "hola", null));

        verify(outbound).send(eq(phone), eq(TO), eq(conversation.getId()), any());
    }

    @Test
    void rejectsAnUnknownClient() {
        assertThatThrownBy(() -> service.send("no-es-uuid", new SendMessageRequest(TO, "hola", null)))
                .isInstanceOf(UnknownClientException.class);
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> service.send(other.toString(), new SendMessageRequest(TO, "hola", null)))
                .isInstanceOf(UnknownClientException.class);
        verify(outbound, never()).send(any(), anyString(), any(), any());
    }

    @Test
    void requiresPhoneNumberIdWhenThereAreSeveralNumbers() {
        PhoneNumber first = phone();
        PhoneNumber second = phone();
        when(phoneNumberRepository.findByBotIdInAndActiveTrue(List.of(BOT_ID))).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.send(APP_ID.toString(), new SendMessageRequest(TO, "hola", null)))
                .isInstanceOf(BadRequestException.class);

        service.send(APP_ID.toString(), new SendMessageRequest(TO, "hola", second.getId()));
        verify(outbound).send(eq(second), eq(TO), any(), any());
    }

    @Test
    void rejectsANumberOfAnotherApplication() {
        PhoneNumber own = phone();
        when(phoneNumberRepository.findByBotIdInAndActiveTrue(List.of(BOT_ID))).thenReturn(List.of(own));

        assertThatThrownBy(() -> service.send(APP_ID.toString(),
                new SendMessageRequest(TO, "hola", UUID.randomUUID())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsWhenTheApplicationHasNoNumbers() {
        when(phoneNumberRepository.findByBotIdInAndActiveTrue(List.of(BOT_ID))).thenReturn(List.of());

        assertThatThrownBy(() -> service.send(APP_ID.toString(), new SendMessageRequest(TO, "hola", null)))
                .isInstanceOf(BadRequestException.class);
    }

    private static PhoneNumber phone() {
        PhoneNumber phone = mock(PhoneNumber.class);
        when(phone.getId()).thenReturn(UUID.randomUUID());
        return phone;
    }
}
