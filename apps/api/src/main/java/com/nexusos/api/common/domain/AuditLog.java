package com.nexusos.api.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private String action;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "source", length = 30)
    private String source = "USER";

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    protected AuditLog() {}

    public AuditLog(String action, UUID userId, Instant timestamp) {
        this.action = action;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public AuditLog(String action, UUID userId, Instant timestamp, String source, String metadata) {
        this.action = action;
        this.userId = userId;
        this.timestamp = timestamp;
        this.source = source;
        this.metadata = metadata;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
