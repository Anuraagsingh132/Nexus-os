package com.nexusos.api.files.domain;

import com.nexusos.api.common.domain.BaseEntity;
import com.nexusos.api.workspace.domain.Workspace;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.SQLRestriction;
import java.time.Instant;

@Entity
@Table(name = "files")
@SQLRestriction("deleted_at IS NULL")
public class FileMetadata extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonIgnore
    private Workspace workspace;

    @Column(nullable = false)
    private String name;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "ingestion_status")
    private String ingestionStatus = "PENDING";

    @Column(name = "ingestion_error", columnDefinition = "TEXT")
    private String ingestionError;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected FileMetadata() {}

    public FileMetadata(Workspace workspace, String name, String objectKey, Long sizeBytes, String contentType) {
        this.workspace = workspace;
        this.name = name;
        this.objectKey = objectKey;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
    }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getIngestionStatus() { return ingestionStatus; }
    public void setIngestionStatus(String ingestionStatus) { this.ingestionStatus = ingestionStatus; }
    public String getIngestionError() { return ingestionError; }
    public void setIngestionError(String ingestionError) { this.ingestionError = ingestionError; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
