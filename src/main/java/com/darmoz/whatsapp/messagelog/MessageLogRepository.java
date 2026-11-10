package com.darmoz.whatsapp.messagelog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageLogRepository extends JpaRepository<MessageLog, UUID> {

    Optional<MessageLog> findByWaMessageId(String waMessageId);

    /**
     * Registra un mensaje entrante salvo que ya este (Meta reintenta el
     * webhook): devuelve 0 si el wamid ya existia y hay que ignorarlo.
     */
    @Modifying
    @Query(value = """
            INSERT INTO wa_message_log (id, conversation_id, phone_number_id, direction, wa_message_id, contact_wa_id,
                                        type, payload, status, created_at, updated_at)
            VALUES (:id, :conversationId, :phoneNumberId, 'IN', :waMessageId, :contactWaId,
                    :type, cast(:payload as jsonb), 'RECEIVED', now(), now())
            ON CONFLICT (wa_message_id) DO NOTHING
            """, nativeQuery = true)
    int insertInboundIfAbsent(@Param("id") UUID id,
                              @Param("conversationId") UUID conversationId,
                              @Param("phoneNumberId") UUID phoneNumberId,
                              @Param("waMessageId") String waMessageId,
                              @Param("contactWaId") String contactWaId,
                              @Param("type") String type,
                              @Param("payload") String payload);

    List<MessageLog> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("""
            select m from MessageLog m
            where (:phoneNumberId is null or m.phoneNumberId = :phoneNumberId)
              and (:contact is null or m.contactWaId = :contact)
              and (:direction is null or m.direction = :direction)
              and (:status is null or m.status = :status)
              and (cast(:from as Instant) is null or m.createdAt >= :from)
              and (cast(:to as Instant) is null or m.createdAt < :to)
            order by m.createdAt desc
            """)
    Page<MessageLog> search(@Param("phoneNumberId") UUID phoneNumberId,
                            @Param("contact") String contact,
                            @Param("direction") MessageDirection direction,
                            @Param("status") MessageStatus status,
                            @Param("from") Instant from,
                            @Param("to") Instant to,
                            Pageable pageable);
}
