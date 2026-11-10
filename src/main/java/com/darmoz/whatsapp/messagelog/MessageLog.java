package com.darmoz.whatsapp.messagelog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "wa_message_log")
public class MessageLog {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "phone_number_id")
    private UUID phoneNumberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private MessageDirection direction;

    @Column(name = "wa_message_id", unique = true, length = 128)
    private String waMessageId;

    @Column(name = "contact_wa_id", length = 32)
    private String contactWaId;

    @Column(nullable = false, length = 30)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(UUID phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public void setDirection(MessageDirection direction) {
        this.direction = direction;
    }

    public String getWaMessageId() {
        return waMessageId;
    }

    public void setWaMessageId(String waMessageId) {
        this.waMessageId = waMessageId;
    }

    public String getContactWaId() {
        return contactWaId;
    }

    public void setContactWaId(String contactWaId) {
        this.contactWaId = contactWaId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
