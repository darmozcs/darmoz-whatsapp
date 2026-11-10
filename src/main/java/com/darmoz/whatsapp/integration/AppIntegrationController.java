package com.darmoz.whatsapp.integration;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/integrations")
public class AppIntegrationController {

    private final AppIntegrationService integrationService;

    AppIntegrationController(AppIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping
    ResponseEntity<AppIntegrationResponse> create(@Valid @RequestBody AppIntegrationRequests.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(integrationService.create(request));
    }

    @PatchMapping("/{id}")
    AppIntegrationResponse update(@PathVariable UUID id, @Valid @RequestBody AppIntegrationRequests.Update request) {
        return integrationService.update(id, request);
    }

    @GetMapping
    List<AppIntegrationResponse> list() {
        return integrationService.list();
    }

    @GetMapping("/{id}")
    AppIntegrationResponse get(@PathVariable UUID id) {
        return integrationService.getById(id);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        integrationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    AppIntegrationService.IntegrationTestResponse test(@PathVariable UUID id,
                                                       @Valid @RequestBody AppIntegrationRequests.Test request) {
        return integrationService.test(id, request);
    }
}
