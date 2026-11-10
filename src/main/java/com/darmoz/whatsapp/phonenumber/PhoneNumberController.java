package com.darmoz.whatsapp.phonenumber;

import com.darmoz.whatsapp.messagelog.MessageLogResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Los numeros se dan de alta con POST /accounts/{id}/sync-numbers. */
@RestController
@RequestMapping("/phone-numbers")
public class PhoneNumberController {

    private final PhoneNumberService phoneNumberService;

    PhoneNumberController(PhoneNumberService phoneNumberService) {
        this.phoneNumberService = phoneNumberService;
    }

    @GetMapping
    List<PhoneNumberResponse> list(@RequestParam(required = false) UUID accountId,
                                   @RequestParam(required = false) UUID botId) {
        return phoneNumberService.list(accountId, botId);
    }

    @PatchMapping("/{id}")
    PhoneNumberResponse update(@PathVariable UUID id, @RequestBody PhoneNumberUpdateRequest request) {
        return phoneNumberService.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        phoneNumberService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test-message")
    MessageLogResponse testMessage(@PathVariable UUID id, @Valid @RequestBody TestMessageRequest request) {
        return phoneNumberService.sendTestMessage(id, request);
    }
}
