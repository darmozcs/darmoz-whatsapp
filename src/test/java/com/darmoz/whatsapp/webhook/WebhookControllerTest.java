package com.darmoz.whatsapp.webhook;

import com.darmoz.whatsapp.account.MetaAccount;
import com.darmoz.whatsapp.account.MetaAccountRepository;
import com.darmoz.whatsapp.meta.WebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final String BODY = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";

    private final MetaAccountRepository accountRepository = mock(MetaAccountRepository.class);
    private final WebhookProcessor processor = mock(WebhookProcessor.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MetaAccount account = new MetaAccount();
        account.setVerifyToken("verify-123");
        account.setAppSecret("app-secret");
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(accountRepository, processor)).build();
    }

    @Test
    void answersTheChallengeWithTheRightVerifyToken() throws Exception {
        mockMvc.perform(get("/webhook/{id}", ACCOUNT_ID)
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "verify-123")
                        .param("hub.challenge", "42"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    void rejectsAWrongVerifyToken() throws Exception {
        mockMvc.perform(get("/webhook/{id}", ACCOUNT_ID)
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "otro")
                        .param("hub.challenge", "42"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsSignedPayloadsAndProcessesThemAsync() throws Exception {
        byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(post("/webhook/{id}", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", WebhookSignatureVerifier.signatureHeader(body, "app-secret"))
                        .content(body))
                .andExpect(status().isOk());

        verify(processor).processAsync(eq(ACCOUNT_ID), any());
    }

    @Test
    void rejectsUnsignedPayloads() throws Exception {
        mockMvc.perform(post("/webhook/{id}", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=00")
                        .content(BODY))
                .andExpect(status().isUnauthorized());

        verify(processor, never()).processAsync(any(), any());
    }
}
