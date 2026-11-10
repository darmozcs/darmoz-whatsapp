package com.darmoz.whatsapp.bot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "wa_bot")
public class Bot {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    /** Application de darmoz-auth a la que pertenece (sin FK: otro servicio). */
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "entry_flow_id")
    private UUID entryFlowId;

    /** Lo que responde si no entiende y no hay flujo para seguir. */
    @Column(name = "fallback_message", columnDefinition = "TEXT")
    private String fallbackMessage;

    /** Minutos sin mensajes del contacto despues de los cuales la conversacion arranca de cero. */
    @Column(name = "session_timeout_minutes", nullable = false)
    private int sessionTimeoutMinutes = 60;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_hours", columnDefinition = "jsonb")
    private BusinessHours businessHours;

    @Column(name = "out_of_hours_message", columnDefinition = "TEXT")
    private String outOfHoursMessage;

    /** Constantes del bot, disponibles en los flujos como {{bot.clave}}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "static_variables", columnDefinition = "jsonb")
    private Map<String, String> staticVariables;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getEntryFlowId() {
        return entryFlowId;
    }

    public void setEntryFlowId(UUID entryFlowId) {
        this.entryFlowId = entryFlowId;
    }

    public String getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public BusinessHours getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(BusinessHours businessHours) {
        this.businessHours = businessHours;
    }

    public String getOutOfHoursMessage() {
        return outOfHoursMessage;
    }

    public void setOutOfHoursMessage(String outOfHoursMessage) {
        this.outOfHoursMessage = outOfHoursMessage;
    }

    public Map<String, String> getStaticVariables() {
        return staticVariables;
    }

    public void setStaticVariables(Map<String, String> staticVariables) {
        this.staticVariables = staticVariables;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
