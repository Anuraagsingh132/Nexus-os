package com.nexusos.api.workspace.domain;

import com.nexusos.api.common.domain.BaseEntity;
import com.nexusos.api.identity.domain.User;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "memberships")
public class Membership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonIgnore
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected Membership() {}

    public Membership(User user, Workspace workspace, Role role) {
        this.user = user;
        this.workspace = workspace;
        this.role = role;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
