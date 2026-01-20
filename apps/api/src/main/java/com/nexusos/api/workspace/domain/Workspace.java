package com.nexusos.api.workspace.domain;

import com.nexusos.api.common.domain.BaseEntity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "workspaces")
@SQLRestriction("deleted_at IS NULL")
public class Workspace extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "agent_enabled", nullable = false)
    private Boolean agentEnabled = true;

    @Column(name = "agent_mode", nullable = false, length = 30)
    private String agentMode = "FULL_AGENT";

    protected Workspace() {}

    public Workspace(Organization organization, String name, String slug) {
        this.organization = organization;
        this.name = name;
        this.slug = slug;
    }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Boolean getAgentEnabled() { return agentEnabled; }
    public void setAgentEnabled(Boolean agentEnabled) { this.agentEnabled = agentEnabled; }
    public String getAgentMode() { return agentMode; }
    public void setAgentMode(String agentMode) { this.agentMode = agentMode; }
}
