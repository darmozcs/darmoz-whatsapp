package com.darmoz.whatsapp.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "wa_conversation")
public class Conversation {

    /** Lo asigna ConversationRepository.insertIfAbsent (INSERT ... ON CONFLICT). */
    @Id
    private UUID id;

    @Column(name = "phone_number_id", nullable = false, updatable = false)
    private UUID phoneNumberId;

    @Column(name = "contact_wa_id", nullable = false, updatable = false, length = 32)
    private String contactWaId;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "current_flow_id")
    private UUID currentFlowId;

    @Column(name = "current_node_id", length = 64)
    private String currentNodeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> variables = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "last_inbound_at")
    private Instant lastInboundAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Conversation() {
    }

    /** Solo para tests: en produccion la crea ConversationRepository.insertIfAbsent. */
    public Conversation(UUID id, UUID phoneNumberId, String contactWaId) {
        this.id = id;
        this.phoneNumberId = phoneNumberId;
        this.contactWaId = contactWaId;
    }

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

    /** Vuelve al flujo de entrada sin variables (sesion vencida, reinicio manual). */
    public void reset() {
        currentFlowId = null;
        currentNodeId = null;
        variables = new HashMap<>();
        status = ConversationStatus.ACTIVE;
    }

    public void close() {
        currentNodeId = null;
        status = ConversationStatus.CLOSED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPhoneNumberId() {
        return phoneNumberId;
    }

    public String getContactWaId() {
        return contactWaId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public UUID getCurrentFlowId() {
        return currentFlowId;
    }

    public void setCurrentFlowId(UUID currentFlowId) {
        this.currentFlowId = currentFlowId;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public Map<String, Object> getVariables() {
        return variables == null ? Map.of() : variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationStatus status) {
        this.status = status;
    }

    public Instant getLastInboundAt() {
        return lastInboundAt;
    }

    public void setLastInboundAt(Instant lastInboundAt) {
        this.lastInboundAt = lastInboundAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
