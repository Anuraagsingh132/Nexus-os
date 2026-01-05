package com.nexusos.api.content.domain;

import com.nexusos.api.common.domain.BaseEntity;
import com.nexusos.api.workspace.domain.Workspace;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.SQLRestriction;
import java.time.Instant;

@Entity
@Table(name = "documents")
@SQLRestriction("deleted_at IS NULL")
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonIgnore
    private Workspace workspace;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Document() {}

    public Document(Workspace workspace, String title, String content) {
        this.workspace = workspace;
        this.title = title;
        this.content = content;
    }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
