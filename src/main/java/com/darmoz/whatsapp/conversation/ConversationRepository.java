package com.darmoz.whatsapp.conversation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Crea la conversacion si no existe; seguro con dos webhooks simultaneos del mismo contacto. */
    @Modifying
    @Query(value = """
            INSERT INTO wa_conversation (id, phone_number_id, contact_wa_id, variables, status, created_at, updated_at)
            VALUES (:id, :phoneNumberId, :contactWaId, '{}'::jsonb, 'ACTIVE', now(), now())
            ON CONFLICT (phone_number_id, contact_wa_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("phoneNumberId") UUID phoneNumberId,
                       @Param("contactWaId") String contactWaId);

    /** SELECT ... FOR UPDATE: los mensajes de un mismo contacto se procesan de a uno. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.phoneNumberId = :phoneNumberId and c.contactWaId = :contactWaId")
    Optional<Conversation> lockByContact(@Param("phoneNumberId") UUID phoneNumberId,
                                         @Param("contactWaId") String contactWaId);

    Optional<Conversation> findByPhoneNumberIdAndContactWaId(UUID phoneNumberId, String contactWaId);

    @Query("""
            select c from Conversation c
            where (:phoneNumberId is null or c.phoneNumberId = :phoneNumberId)
              and (:contact is null or c.contactWaId = :contact)
              and (:status is null or c.status = :status)
            order by c.updatedAt desc
            """)
    Page<Conversation> search(@Param("phoneNumberId") UUID phoneNumberId,
                              @Param("contact") String contact,
                              @Param("status") ConversationStatus status,
                              Pageable pageable);
}
