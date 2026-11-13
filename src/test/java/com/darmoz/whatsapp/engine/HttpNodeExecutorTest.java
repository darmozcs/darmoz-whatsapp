package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.account.MetaAccount;
import com.darmoz.whatsapp.flow.NodeType;
import com.darmoz.whatsapp.integration.AppHttpCaller;
import com.darmoz.whatsapp.integration.AppIntegration;
import com.darmoz.whatsapp.integration.AppIntegrationRepository;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpNodeExecutorTest {

    private static final UUID APP_ID = UUID.randomUUID();

    private final AppIntegrationRepository integrationRepository = mock(AppIntegrationRepository.class);
    private final AppHttpCaller httpCaller = mock(AppHttpCaller.class);
    private final HttpNodeExecutor executor = new HttpNodeExecutor(integrationRepository, httpCaller);

    @Test
    @SuppressWarnings("unchecked")
    void sendsTheWhatsappHeadersAndTheNodeCannotOverrideThem() {
        AppIntegration integration = new AppIntegration();
        when(integrationRepository.findByApplicationId(APP_ID)).thenReturn(Optional.of(integration));
        when(httpCaller.call(eq(integration), eq("GET"), eq("/bot/me"), any(), any()))
                .thenReturn(new AppHttpCaller.HttpCallResult(200, "{\"fullName\":\"Ana\"}"));

        MetaAccount account = new MetaAccount();
        account.setWabaId("2695094194284681");
        PhoneNumber phoneNumber = mock(PhoneNumber.class);
        when(phoneNumber.getAccount()).thenReturn(account);
        when(phoneNumber.getPhoneNumberId()).thenReturn("1443667295501792");

        Map<String, Object> data = Map.of(
                "method", "GET",
                "path", "/bot/me",
                "saveAs", "clienta",
                "headers", Map.of("x-whatsapp-waba-id", "999", "X-Whatsapp-From", "123", "X-Extra", "{{bot.tienda}}"));
        Map<String, Object> variables = new HashMap<>();
        VariableScope scope = new VariableScope("59899123456", "Ana", Map.of("tienda", "centro"), variables);

        String handle = executor.execute(data, APP_ID, phoneNumber, scope, variables);

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(httpCaller).call(eq(integration), eq("GET"), eq("/bot/me"), headers.capture(), any());
        assertThat(headers.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "X-Extra", "centro",
                "X-Whatsapp-From", "59899123456",
                "X-Whatsapp-Waba-Id", "2695094194284681",
                "X-Whatsapp-Phone-Number-Id", "1443667295501792"));
        assertThat(handle).isEqualTo(NodeType.HANDLE_SUCCESS);
        assertThat((Map<String, Object>) variables.get("clienta")).containsEntry("fullName", "Ana");
    }
}
