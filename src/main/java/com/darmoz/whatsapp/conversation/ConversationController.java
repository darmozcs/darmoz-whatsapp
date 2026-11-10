package com.darmoz.whatsapp.conversation;

import com.darmoz.whatsapp.common.NotFoundException;
import com.darmoz.whatsapp.common.PageResponse;
import com.darmoz.whatsapp.messagelog.MessageLogRepository;
import com.darmoz.whatsapp.messagelog.MessageLogResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final MessageLogRepository messageLogRepository;

    ConversationController(ConversationRepository conversationRepository, MessageLogRepository messageLogRepository) {
        this.conversationRepository = conversationRepository;
        this.messageLogRepository = messageLogRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    PageResponse<ConversationResponse> search(@RequestParam(required = false) UUID phoneNumberId,
                                              @RequestParam(required = false) String contact,
                                              @RequestParam(required = false) ConversationStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        String contactFilter = contact == null || contact.isBlank() ? null : contact.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        return PageResponse.from(conversationRepository.search(phoneNumberId, contactFilter, status, pageable),
                ConversationResponse::from);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    ConversationResponse get(@PathVariable UUID id) {
        return ConversationResponse.from(findOrThrow(id));
    }

    @GetMapping("/{id}/messages")
    @Transactional(readOnly = true)
    List<MessageLogResponse> messages(@PathVariable UUID id) {
        findOrThrow(id);
        return messageLogRepository.findByConversationIdOrderByCreatedAtAsc(id).stream()
                .map(MessageLogResponse::from).toList();
    }

    /** Vuelve al flujo de entrada y saca el HANDOFF: el proximo mensaje lo atiende el bot. */
    @PostMapping("/{id}/reset")
    @Transactional
    ConversationResponse reset(@PathVariable UUID id) {
        Conversation conversation = findOrThrow(id);
        conversation.reset();
        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    private Conversation findOrThrow(UUID id) {
        return conversationRepository.findById(id).orElseThrow(() -> new NotFoundException("Conversacion", id));
    }
}
