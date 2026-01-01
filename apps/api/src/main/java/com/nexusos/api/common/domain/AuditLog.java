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

    protected AuditLog() {}

    public AuditLog(String action, UUID userId, Instant timestamp) {
        this.action = action;
        this.userId = userId;
        this.timestamp = timestamp;
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
}
