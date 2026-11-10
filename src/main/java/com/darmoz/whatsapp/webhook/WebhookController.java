package com.darmoz.whatsapp.webhook;

import com.darmoz.whatsapp.account.MetaAccount;
import com.darmoz.whatsapp.account.MetaAccountRepository;
import com.darmoz.whatsapp.meta.WebhookSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Callback que se configura en Meta, uno por cuenta: /webhook/{accountId}.
 * El POST responde 200 enseguida (si Meta no recibe respuesta rapido,
 * reintenta) y el procesamiento sigue en WebhookProcessor (async).
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private final MetaAccountRepository accountRepository;
    private final WebhookProcessor webhookProcessor;

    WebhookController(MetaAccountRepository accountRepository, WebhookProcessor webhookProcessor) {
        this.accountRepository = accountRepository;
        this.webhookProcessor = webhookProcessor;
    }

    /** Verificacion de Meta al guardar el webhook: devolver hub.challenge si el verify token coincide. */
    @GetMapping(value = "/{accountId}", produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> verify(@PathVariable UUID accountId,
                                  @RequestParam(name = "hub.mode", required = false) String mode,
                                  @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
                                  @RequestParam(name = "hub.challenge", required = false) String challenge) {
        Optional<MetaAccount> account = accountRepository.findById(accountId);
        if ("subscribe".equals(mode) && verifyToken != null && challenge != null && account.isPresent()
                && MessageDigest.isEqual(verifyToken.getBytes(StandardCharsets.UTF_8),
                account.get().getVerifyToken().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.ok(challenge);
        }
        log.warn("Verificacion de webhook rechazada para la cuenta {}", accountId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/{accountId}")
    ResponseEntity<Void> receive(@PathVariable UUID accountId,
                                 @RequestHeader(name = SIGNATURE_HEADER, required = false) String signature,
                                 @RequestBody byte[] body) {
        Optional<MetaAccount> account = accountRepository.findById(accountId).filter(MetaAccount::isActive);
        if (account.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!WebhookSignatureVerifier.isValid(body, signature, account.get().getAppSecret())) {
            log.warn("Firma invalida en el webhook de la cuenta {}", accountId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        webhookProcessor.processAsync(accountId, body);
        return ResponseEntity.ok().build();
    }
}
