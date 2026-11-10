package com.darmoz.whatsapp.integration;

import com.darmoz.whatsapp.common.ConflictException;
import com.darmoz.whatsapp.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AppIntegrationService {

    private static final int RESPONSE_PREVIEW_CHARS = 2000;

    private final AppIntegrationRepository integrationRepository;
    private final AppHttpCaller httpCaller;

    AppIntegrationService(AppIntegrationRepository integrationRepository, AppHttpCaller httpCaller) {
        this.integrationRepository = integrationRepository;
        this.httpCaller = httpCaller;
    }

    @Transactional
    public AppIntegrationResponse create(AppIntegrationRequests.Create request) {
        if (integrationRepository.existsByApplicationId(request.applicationId())) {
            throw new ConflictException("Ya existe una integracion para esa aplicacion");
        }
        AppIntegration integration = new AppIntegration();
        integration.setApplicationId(request.applicationId());
        integration.setApplicationName(request.applicationName().trim());
        integration.setBaseUrl(request.baseUrl().trim());
        integration.setAuthHeaderName(blankToNull(request.authHeaderName()));
        integration.setAuthHeaderValue(integration.getAuthHeaderName() == null ? null : blankToNull(request.authHeaderValue()));
        if (request.timeoutMs() != null) {
            integration.setTimeoutMs(request.timeoutMs());
        }
        return AppIntegrationResponse.from(integrationRepository.save(integration));
    }

    @Transactional
    public AppIntegrationResponse update(UUID id, AppIntegrationRequests.Update request) {
        AppIntegration integration = findOrThrow(id);
        if (!isBlank(request.applicationName())) {
            integration.setApplicationName(request.applicationName().trim());
        }
        if (!isBlank(request.baseUrl())) {
            integration.setBaseUrl(request.baseUrl().trim());
        }
        if (Boolean.TRUE.equals(request.clearAuthHeader())) {
            integration.setAuthHeaderName(null);
            integration.setAuthHeaderValue(null);
        } else {
            if (!isBlank(request.authHeaderName())) {
                integration.setAuthHeaderName(request.authHeaderName().trim());
            }
            if (!isBlank(request.authHeaderValue())) {
                integration.setAuthHeaderValue(request.authHeaderValue().trim());
            }
        }
        if (request.timeoutMs() != null) {
            integration.setTimeoutMs(request.timeoutMs());
        }
        return AppIntegrationResponse.from(integrationRepository.save(integration));
    }

    @Transactional(readOnly = true)
    public List<AppIntegrationResponse> list() {
        return integrationRepository.findAll().stream().map(AppIntegrationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppIntegrationResponse getById(UUID id) {
        return AppIntegrationResponse.from(findOrThrow(id));
    }

    @Transactional
    public void delete(UUID id) {
        integrationRepository.delete(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public IntegrationTestResponse test(UUID id, AppIntegrationRequests.Test request) {
        AppIntegration integration = findOrThrow(id);
        String method = request.method() == null ? "GET" : request.method();
        AppHttpCaller.HttpCallResult result = httpCaller.call(integration, method, request.path(), null, null);
        String body = result.body();
        return new IntegrationTestResponse(result.status(),
                body.length() > RESPONSE_PREVIEW_CHARS ? body.substring(0, RESPONSE_PREVIEW_CHARS) + "…" : body);
    }

    private AppIntegration findOrThrow(UUID id) {
        return integrationRepository.findById(id).orElseThrow(() -> new NotFoundException("Integracion", id));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public record IntegrationTestResponse(int status, String body) {
    }
}
