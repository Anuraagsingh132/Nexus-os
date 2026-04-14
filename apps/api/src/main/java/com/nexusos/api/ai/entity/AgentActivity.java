package com.nexusos.api.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_activities")
public class AgentActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "arguments_json", columnDefinition = "TEXT")
    private String argumentsJson;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "requires_confirmation", nullable = false)
    private Boolean requiresConfirmation;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "source_channel_id")
    private UUID sourceChannelId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public AgentActivity() {
    }

    public AgentActivity(UUID id, UUID workspaceId, UUID requestedBy, String toolName, String argumentsJson, String status, String resultSummary, String errorMessage, Boolean requiresConfirmation, UUID confirmedBy, UUID sourceChannelId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.requestedBy = requestedBy;
        this.toolName = toolName;
        this.argumentsJson = argumentsJson;
        this.status = status;
        this.resultSummary = resultSummary;
        this.errorMessage = errorMessage;
        this.requiresConfirmation = requiresConfirmation;
        this.confirmedBy = confirmedBy;
        this.sourceChannelId = sourceChannelId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    
    public UUID getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UUID requestedBy) { this.requestedBy = requestedBy; }
    
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    
    public String getArgumentsJson() { return argumentsJson; }
    public void setArgumentsJson(String argumentsJson) { this.argumentsJson = argumentsJson; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Boolean getRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(Boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
    
    public UUID getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(UUID confirmedBy) { this.confirmedBy = confirmedBy; }
    
    public UUID getSourceChannelId() { return sourceChannelId; }
    public void setSourceChannelId(UUID sourceChannelId) { this.sourceChannelId = sourceChannelId; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID workspaceId;
        private UUID requestedBy;
        private String toolName;
        private String argumentsJson;
        private String status;
        private String resultSummary;
        private String errorMessage;
        private Boolean requiresConfirmation;
        private UUID confirmedBy;
        private UUID sourceChannelId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder workspaceId(UUID workspaceId) { this.workspaceId = workspaceId; return this; }
        public Builder requestedBy(UUID requestedBy) { this.requestedBy = requestedBy; return this; }
        public Builder toolName(String toolName) { this.toolName = toolName; return this; }
        public Builder argumentsJson(String argumentsJson) { this.argumentsJson = argumentsJson; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder resultSummary(String resultSummary) { this.resultSummary = resultSummary; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder requiresConfirmation(Boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; return this; }
        public Builder confirmedBy(UUID confirmedBy) { this.confirmedBy = confirmedBy; return this; }
        public Builder sourceChannelId(UUID sourceChannelId) { this.sourceChannelId = sourceChannelId; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AgentActivity build() {
            return new AgentActivity(id, workspaceId, requestedBy, toolName, argumentsJson, status, resultSummary, errorMessage, requiresConfirmation, confirmedBy, sourceChannelId, createdAt, updatedAt);
        }
    }
}
