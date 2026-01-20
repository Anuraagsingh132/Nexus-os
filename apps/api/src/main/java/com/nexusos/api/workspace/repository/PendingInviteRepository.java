package com.nexusos.api.workspace.repository;

import com.nexusos.api.workspace.domain.PendingInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PendingInviteRepository extends JpaRepository<PendingInvite, UUID> {
    Optional<PendingInvite> findByWorkspaceIdAndEmail(UUID workspaceId, String email);
    List<PendingInvite> findByEmail(String email);
    void deleteByWorkspaceIdAndEmail(UUID workspaceId, String email);
}
