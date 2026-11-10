package com.darmoz.whatsapp.messagelog;

import com.darmoz.whatsapp.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/message-logs")
public class MessageLogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MessageLogRepository messageLogRepository;

    MessageLogController(MessageLogRepository messageLogRepository) {
        this.messageLogRepository = messageLogRepository;
    }

    @GetMapping
    PageResponse<MessageLogResponse> search(
            @RequestParam(required = false) UUID phoneNumberId,
            @RequestParam(required = false) String contact,
            @RequestParam(required = false) MessageDirection direction,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String contactFilter = contact == null || contact.isBlank() ? null : contact.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageResponse.from(
                messageLogRepository.search(phoneNumberId, contactFilter, direction, status, from, to, pageable),
                MessageLogResponse::from);
    }
}
