package com.darmoz.whatsapp.meta;

import com.darmoz.whatsapp.account.MetaAccount;
import com.darmoz.whatsapp.common.ExternalApiException;
import com.darmoz.whatsapp.config.WhatsappProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Graph API de Meta (WhatsApp Cloud API). Cada llamada usa el token y la
 * version de la cuenta que se le pasa.
 */
@Component
public class WhatsAppCloudClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public WhatsAppCloudClient(RestClient.Builder builder, WhatsappProperties properties) {
        this.restClient = builder.baseUrl(properties.graphApiBaseUrl()).build();
    }

    /** GET /{waba}: valida el token y devuelve el nombre de la WABA. */
    public String getWabaName(MetaAccount account) {
        Map<String, Object> body = get(account, "/{version}/{waba}?fields=id,name",
                account.getGraphApiVersion(), account.getWabaId());
        return body == null ? null : (String) body.get("name");
    }

    public List<MetaPhoneNumber> getPhoneNumbers(MetaAccount account) {
        Map<String, Object> body = get(account,
                "/{version}/{waba}/phone_numbers?fields=id,display_phone_number,verified_name",
                account.getGraphApiVersion(), account.getWabaId());
        Object data = body == null ? null : body.get("data");
        if (!(data instanceof List<?> items)) {
            return List.of();
        }
        return items.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<?, ?>) item)
                .map(item -> new MetaPhoneNumber(
                        String.valueOf(item.get("id")),
                        (String) item.get("display_phone_number"),
                        (String) item.get("verified_name")))
                .toList();
    }

    /** POST /{waba}/subscribed_apps: que la app reciba los eventos de la WABA. */
    public void subscribeApp(MetaAccount account) {
        post(account, "/{version}/{waba}/subscribed_apps", Map.of(),
                account.getGraphApiVersion(), account.getWabaId());
    }

    /**
     * POST /{phoneNumberId}/messages. El payload es el objeto de Meta sin
     * messaging_product ni to (los agrega aca). Devuelve el wamid.
     */
    public String sendMessage(MetaAccount account, String phoneNumberId, String to, Map<String, Object> payload) {
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("messaging_product", "whatsapp");
        request.put("recipient_type", "individual");
        request.put("to", to);
        request.putAll(payload);

        Map<String, Object> body = post(account, "/{version}/{phoneNumberId}/messages", request,
                account.getGraphApiVersion(), phoneNumberId);
        Object messages = body == null ? null : body.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> first) {
            return (String) first.get("id");
        }
        return null;
    }

    private Map<String, Object> get(MetaAccount account, String uri, Object... uriVariables) {
        try {
            return restClient.get()
                    .uri(uri, uriVariables)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.getAccessToken())
                    .retrieve()
                    .body(JSON_MAP);
        } catch (RestClientResponseException e) {
            throw new ExternalApiException("Meta respondio " + e.getStatusCode().value() + ": "
                    + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new ExternalApiException("No se pudo llamar a la Graph API de Meta", e);
        }
    }

    private Map<String, Object> post(MetaAccount account, String uri, Object body, Object... uriVariables) {
        try {
            return restClient.post()
                    .uri(uri, uriVariables)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.getAccessToken())
                    .body(body)
                    .retrieve()
                    .body(JSON_MAP);
        } catch (RestClientResponseException e) {
            throw new ExternalApiException("Meta respondio " + e.getStatusCode().value() + ": "
                    + e.getResponseBodyAsString(), e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new ExternalApiException("No se pudo llamar a la Graph API de Meta", e);
        }
    }
}
