package com.darmoz.whatsapp.flow;

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
import java.util.UUID;

/**
 * El editor guarda en draftDefinition; el motor solo ejecuta
 * publishedDefinition, asi editar no rompe las conversaciones en curso.
 */
@Entity
@Table(name = "wa_flow")
public class Flow {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "bot_id", nullable = false, updatable = false)
    private UUID botId;

    @Column(nullable = false, length = 150)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draft_definition", nullable = false, columnDefinition = "jsonb")
    private FlowDefinition draftDefinition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "published_definition", columnDefinition = "jsonb")
    private FlowDefinition publishedDefinition;

    @Column(name = "published_version", nullable = false)
    private int publishedVersion;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Flow() {
    }

    public Flow(UUID botId) {
        this.botId = botId;
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

    public void publish() {
        publishedDefinition = draftDefinition;
        publishedVersion++;
        publishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBotId() {
        return botId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FlowDefinition getDraftDefinition() {
        return draftDefinition;
    }

    public void setDraftDefinition(FlowDefinition draftDefinition) {
        this.draftDefinition = draftDefinition;
    }

    public FlowDefinition getPublishedDefinition() {
        return publishedDefinition;
    }

    public int getPublishedVersion() {
        return publishedVersion;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
