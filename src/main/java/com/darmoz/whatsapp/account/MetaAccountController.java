package com.darmoz.whatsapp.account;

import com.darmoz.whatsapp.phonenumber.PhoneNumberResponse;
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
@RequestMapping("/accounts")
public class MetaAccountController {

    private final MetaAccountService accountService;

    MetaAccountController(MetaAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    ResponseEntity<MetaAccountResponse> create(@Valid @RequestBody MetaAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @PatchMapping("/{id}")
    MetaAccountResponse update(@PathVariable UUID id, @Valid @RequestBody MetaAccountUpdateRequest request) {
        return accountService.update(id, request);
    }

    @GetMapping("/{id}")
    MetaAccountResponse get(@PathVariable UUID id) {
        return accountService.getById(id);
    }

    @GetMapping
    List<MetaAccountResponse> list() {
        return accountService.list();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate")
    AccountValidationResponse validate(@PathVariable UUID id) {
        return accountService.validate(id);
    }

    @PostMapping("/{id}/sync-numbers")
    List<PhoneNumberResponse> syncNumbers(@PathVariable UUID id) {
        return accountService.syncPhoneNumbers(id);
    }

    @PostMapping("/{id}/subscribe")
    ResponseEntity<Void> subscribe(@PathVariable UUID id) {
        accountService.subscribe(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/webhook-info")
    WebhookInfoResponse webhookInfo(@PathVariable UUID id) {
        return accountService.getWebhookInfo(id);
    }

    @PostMapping("/{id}/regenerate-verify-token")
    WebhookInfoResponse regenerateVerifyToken(@PathVariable UUID id) {
        return accountService.regenerateVerifyToken(id);
    }
}
