package com.nexusos.api.ai.service;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingestion_failures")
public class IngestionFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "retried", nullable = false)
    private boolean retried = false;

    protected IngestionFailure() {}

    public IngestionFailure(UUID workspaceId, UUID documentId, String title, String fileType, String errorMessage) {
        this.workspaceId = workspaceId;
        this.documentId = documentId;
        this.title = title;
        this.fileType = fileType;
        this.errorMessage = errorMessage;
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public String getFileType() { return fileType; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRetried() { return retried; }
    public void setRetried(boolean retried) { this.retried = retried; }
}
