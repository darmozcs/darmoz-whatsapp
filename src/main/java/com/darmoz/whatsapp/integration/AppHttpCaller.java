package com.darmoz.whatsapp.integration;

import com.darmoz.whatsapp.common.BadRequestException;
import com.darmoz.whatsapp.common.ExternalApiException;
import com.darmoz.whatsapp.config.WhatsappProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Llama a la API de la aplicacion de un bot. La URL es siempre base_url + path
 * (paths relativos, sin esquema ni ".."), asi un flujo no puede pegarle a
 * cualquier host. El header de la integracion (API key) se agrega siempre y
 * los headers del nodo no lo pueden pisar.
 */
@Component
public class AppHttpCaller {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final int maxResponseBytes;

    public AppHttpCaller(WhatsappProperties properties) {
        this.maxResponseBytes = properties.engine().httpMaxResponseBytes();
    }

    /**
     * No tira error por status HTTP: 4xx/5xx vuelven en el resultado.
     *
     * @throws ExternalApiException si no hubo respuesta (timeout, conexion)
     * @throws BadRequestException  si el path no es valido
     */
    public HttpCallResult call(AppIntegration integration, String method, String path,
                               Map<String, String> headers, String body) {
        URI uri = buildUri(integration.getBaseUrl(), path);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(integration.getTimeoutMs()));
        RestClient client = RestClient.builder().requestFactory(factory).build();

        try {
            RestClient.RequestBodySpec request = client.method(HttpMethod.valueOf(method.toUpperCase()))
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON);
            if (headers != null) {
                headers.forEach((name, value) -> {
                    if (!name.equalsIgnoreCase(integration.getAuthHeaderName())) {
                        request.header(name, value);
                    }
                });
            }
            if (integration.getAuthHeaderName() != null && integration.getAuthHeaderValue() != null) {
                request.header(integration.getAuthHeaderName(), integration.getAuthHeaderValue());
            }
            if (body != null && !body.isBlank()) {
                request.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return request.exchange((req, response) ->
                    new HttpCallResult(response.getStatusCode().value(), readLimited(response.getBody())));
        } catch (RestClientException e) {
            throw new ExternalApiException("No se pudo llamar a " + uri + ": " + e.getMessage(), e);
        }
    }

    static URI buildUri(String baseUrl, String path) {
        if (path == null || path.isBlank()) {
            path = "/";
        }
        String trimmed = path.trim();
        if (trimmed.contains("://") || trimmed.startsWith("//") || trimmed.contains("..") || trimmed.contains("\\")) {
            throw new BadRequestException("El path tiene que ser relativo a la URL de la aplicacion: " + path);
        }
        String base = baseUrl.replaceAll("/+$", "");
        String relative = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        try {
            return URI.create(base + relative);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("URL invalida: " + base + relative);
        }
    }

    private String readLimited(InputStream body) throws IOException {
        if (body == null) {
            return "";
        }
        byte[] bytes = body.readNBytes(maxResponseBytes + 1);
        if (bytes.length > maxResponseBytes) {
            throw new IOException("La respuesta supera " + maxResponseBytes + " bytes");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record HttpCallResult(int status, String body) {

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }
}
