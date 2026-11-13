package com.darmoz.whatsapp.engine;

import com.darmoz.whatsapp.common.BadRequestException;
import com.darmoz.whatsapp.common.ExternalApiException;
import com.darmoz.whatsapp.flow.NodeData;
import com.darmoz.whatsapp.flow.NodeType;
import com.darmoz.whatsapp.integration.AppHttpCaller;
import com.darmoz.whatsapp.integration.AppIntegration;
import com.darmoz.whatsapp.integration.AppIntegrationRepository;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nodo http: llama a la API de la aplicacion del bot (con su integracion) y
 * guarda la respuesta. Variables que deja:
 * <ul>
 *   <li>saveAs (si se configuro): el JSON de la respuesta, o el texto si no es JSON</li>
 *   <li>cada mapeo variable -> JsonPath (ej. total -> $.total)</li>
 *   <li>httpStatus: el status de la respuesta (0 si no hubo respuesta)</li>
 * </ul>
 * Sale por "success" con 2xx y por "error" con cualquier otra cosa.
 */
@Component
public class HttpNodeExecutor {

    static final String HEADER_WHATSAPP_FROM = "X-Whatsapp-From";
    /** Cuenta de Meta (WABA) que recibio el mensaje: la aplicacion la usa para saber de quien es el bot. */
    static final String HEADER_WHATSAPP_WABA_ID = "X-Whatsapp-Waba-Id";
    static final String HEADER_WHATSAPP_PHONE_NUMBER_ID = "X-Whatsapp-Phone-Number-Id";
    /** Los pone siempre el motor: los headers del nodo no los pueden pisar. */
    private static final Set<String> RESERVED_HEADERS = Set.of(HEADER_WHATSAPP_FROM.toLowerCase(Locale.ROOT),
            HEADER_WHATSAPP_WABA_ID.toLowerCase(Locale.ROOT), HEADER_WHATSAPP_PHONE_NUMBER_ID.toLowerCase(Locale.ROOT));
    static final String VARIABLE_STATUS = "httpStatus";

    private static final Logger log = LoggerFactory.getLogger(HttpNodeExecutor.class);

    private final AppIntegrationRepository integrationRepository;
    private final AppHttpCaller httpCaller;

    public HttpNodeExecutor(AppIntegrationRepository integrationRepository, AppHttpCaller httpCaller) {
        this.integrationRepository = integrationRepository;
        this.httpCaller = httpCaller;
    }

    public String execute(Map<String, Object> data, UUID applicationId, PhoneNumber phoneNumber,
                          VariableScope scope, Map<String, Object> variables) {
        AppIntegration integration = integrationRepository.findByApplicationId(applicationId).orElse(null);
        if (integration == null) {
            log.warn("El bot de la aplicacion {} usa un nodo http pero la aplicacion no tiene integracion", applicationId);
            variables.put(VARIABLE_STATUS, 0);
            return NodeType.HANDLE_ERROR;
        }

        String method = NodeData.string(data, "method");
        String path = TemplateRenderer.render(NodeData.string(data, "path"), scope, TemplateRenderer.Escape.URL);
        String body = TemplateRenderer.render(NodeData.string(data, "body"), scope, TemplateRenderer.Escape.JSON);
        Map<String, String> headers = new LinkedHashMap<>();
        NodeData.stringMap(data, "headers").forEach((name, value) -> {
            if (!RESERVED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                headers.put(name, TemplateRenderer.render(value, scope));
            }
        });
        headers.put(HEADER_WHATSAPP_FROM, scope.contactWaId());
        headers.put(HEADER_WHATSAPP_WABA_ID, phoneNumber.getAccount().getWabaId());
        headers.put(HEADER_WHATSAPP_PHONE_NUMBER_ID, phoneNumber.getPhoneNumberId());

        AppHttpCaller.HttpCallResult result;
        try {
            result = httpCaller.call(integration, method == null ? "GET" : method, path, headers, body);
        } catch (ExternalApiException | BadRequestException e) {
            log.warn("Nodo http contra {} fallo: {}", integration.getApplicationName(), e.getMessage());
            variables.put(VARIABLE_STATUS, 0);
            return NodeType.HANDLE_ERROR;
        }

        variables.put(VARIABLE_STATUS, result.status());
        Object document = parse(result.body());
        String saveAs = NodeData.string(data, "saveAs");
        if (saveAs != null) {
            variables.put(saveAs, document != null ? document : result.body());
        }
        if (document != null) {
            NodeData.stringMap(data, "mappings").forEach((variable, jsonPath) ->
                    variables.put(variable, read(document, jsonPath)));
        }
        return result.isSuccess() ? NodeType.HANDLE_SUCCESS : NodeType.HANDLE_ERROR;
    }

    private static Object parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return Configuration.defaultConfiguration().jsonProvider().parse(body);
        } catch (InvalidJsonException e) {
            return null;
        }
    }

    private static Object read(Object document, String jsonPath) {
        try {
            return JsonPath.read(document, jsonPath.startsWith("$") ? jsonPath : "$." + jsonPath);
        } catch (JsonPathException | IllegalArgumentException e) {
            return null;
        }
    }
}
