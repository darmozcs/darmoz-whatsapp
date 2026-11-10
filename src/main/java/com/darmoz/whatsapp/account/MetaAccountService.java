package com.darmoz.whatsapp.account;

import com.darmoz.whatsapp.common.ExternalApiException;
import com.darmoz.whatsapp.common.NotFoundException;
import com.darmoz.whatsapp.config.WhatsappProperties;
import com.darmoz.whatsapp.meta.MetaPhoneNumber;
import com.darmoz.whatsapp.meta.WhatsAppCloudClient;
import com.darmoz.whatsapp.phonenumber.PhoneNumber;
import com.darmoz.whatsapp.phonenumber.PhoneNumberRepository;
import com.darmoz.whatsapp.phonenumber.PhoneNumberResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class MetaAccountService {

    private static final String SUBSCRIBE_FIELDS = "messages";

    private final MetaAccountRepository accountRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final WhatsAppCloudClient cloudClient;
    private final WhatsappProperties properties;
    private final String contextPath;
    private final SecureRandom random = new SecureRandom();

    MetaAccountService(MetaAccountRepository accountRepository, PhoneNumberRepository phoneNumberRepository,
                       WhatsAppCloudClient cloudClient, WhatsappProperties properties,
                       @Value("${server.servlet.context-path:}") String contextPath) {
        this.accountRepository = accountRepository;
        this.phoneNumberRepository = phoneNumberRepository;
        this.cloudClient = cloudClient;
        this.properties = properties;
        this.contextPath = contextPath;
    }

    @Transactional
    public MetaAccountResponse create(MetaAccountCreateRequest request) {
        MetaAccount account = new MetaAccount();
        account.setName(request.name().trim());
        account.setAppId(blankToNull(request.appId()));
        account.setAppSecret(request.appSecret().trim());
        account.setAccessToken(request.accessToken().trim());
        account.setWabaId(request.wabaId().trim());
        account.setGraphApiVersion(isBlank(request.graphApiVersion())
                ? properties.defaultGraphApiVersion() : request.graphApiVersion().trim());
        account.setActive(request.active() == null || request.active());
        account.setVerifyToken(newVerifyToken());
        return MetaAccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public MetaAccountResponse update(UUID id, MetaAccountUpdateRequest request) {
        MetaAccount account = findOrThrow(id);
        if (!isBlank(request.name())) {
            account.setName(request.name().trim());
        }
        if (request.appId() != null) {
            account.setAppId(blankToNull(request.appId()));
        }
        if (!isBlank(request.appSecret())) {
            account.setAppSecret(request.appSecret().trim());
        }
        if (!isBlank(request.accessToken())) {
            account.setAccessToken(request.accessToken().trim());
            account.setLastValidatedAt(null);
        }
        if (!isBlank(request.wabaId())) {
            account.setWabaId(request.wabaId().trim());
        }
        if (!isBlank(request.graphApiVersion())) {
            account.setGraphApiVersion(request.graphApiVersion().trim());
        }
        if (request.active() != null) {
            account.setActive(request.active());
        }
        return MetaAccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public MetaAccountResponse getById(UUID id) {
        return MetaAccountResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MetaAccountResponse> list() {
        return accountRepository.findAll().stream().map(MetaAccountResponse::from).toList();
    }

    @Transactional
    public void delete(UUID id) {
        accountRepository.delete(findOrThrow(id));
    }

    /** Prueba el token contra la WABA. No tira error: el resultado va en la respuesta. */
    @Transactional
    public AccountValidationResponse validate(UUID id) {
        MetaAccount account = findOrThrow(id);
        try {
            String wabaName = cloudClient.getWabaName(account);
            account.setLastValidatedAt(Instant.now());
            return new AccountValidationResponse(true, wabaName, null, account.getLastValidatedAt());
        } catch (ExternalApiException e) {
            return new AccountValidationResponse(false, null, e.getMessage(), null);
        }
    }

    /** Trae los numeros de la WABA: crea los nuevos y actualiza nombre/numero de los existentes. */
    @Transactional
    public List<PhoneNumberResponse> syncPhoneNumbers(UUID id) {
        MetaAccount account = findOrThrow(id);
        for (MetaPhoneNumber remote : cloudClient.getPhoneNumbers(account)) {
            PhoneNumber phoneNumber = phoneNumberRepository.findByPhoneNumberId(remote.id())
                    .orElseGet(() -> new PhoneNumber(account, remote.id()));
            phoneNumber.setDisplayNumber(remote.displayPhoneNumber());
            phoneNumber.setVerifiedName(remote.verifiedName());
            phoneNumberRepository.save(phoneNumber);
        }
        return phoneNumberRepository.findByAccountId(id).stream().map(PhoneNumberResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public void subscribe(UUID id) {
        cloudClient.subscribeApp(findOrThrow(id));
    }

    @Transactional
    public WebhookInfoResponse regenerateVerifyToken(UUID id) {
        MetaAccount account = findOrThrow(id);
        account.setVerifyToken(newVerifyToken());
        return webhookInfo(account);
    }

    @Transactional(readOnly = true)
    public WebhookInfoResponse getWebhookInfo(UUID id) {
        return webhookInfo(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MetaAccount findOrThrow(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new NotFoundException("Cuenta de Meta", id));
    }

    private WebhookInfoResponse webhookInfo(MetaAccount account) {
        String base = properties.publicBaseUrl().replaceAll("/+$", "");
        return new WebhookInfoResponse(base + contextPath + "/webhook/" + account.getId(),
                account.getVerifyToken(), SUBSCRIBE_FIELDS);
    }

    private String newVerifyToken() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
