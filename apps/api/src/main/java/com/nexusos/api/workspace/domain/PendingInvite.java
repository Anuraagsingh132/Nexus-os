package com.nexusos.api.workspace.domain;

import com.nexusos.api.common.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "pending_invites", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workspace_id", "email"})
})
public class PendingInvite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String email;

    protected PendingInvite() {}

    public PendingInvite(Workspace workspace, String email) {
        this.workspace = workspace;
        this.email = email;
    }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
