package com.darmoz.whatsapp.messaging;

import com.darmoz.whatsapp.messagelog.MessageLogResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint interno para que otros servicios manden mensajes (sin JWT, con X-Client-Id). */
@RestController
@RequestMapping("/messages")
public class SendMessageController {

    private final SendMessageService sendMessageService;

    SendMessageController(SendMessageService sendMessageService) {
        this.sendMessageService = sendMessageService;
    }

    @PostMapping
    MessageLogResponse send(@RequestHeader("X-Client-Id") String clientId,
                            @Valid @RequestBody SendMessageRequest request) {
        return sendMessageService.send(clientId, request);
    }
}
