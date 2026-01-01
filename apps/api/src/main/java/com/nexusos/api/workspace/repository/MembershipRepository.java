package com.nexusos.api.workspace.repository;

import com.nexusos.api.workspace.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
    Optional<Membership> findByWorkspaceIdAndUserEmail(UUID workspaceId, String userEmail);
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
    boolean existsByWorkspaceIdAndUserEmail(UUID workspaceId, String userEmail);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"workspace", "user"})
    java.util.List<Membership> findByUserId(UUID userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"workspace", "user"})
    java.util.List<Membership> findByWorkspaceId(UUID workspaceId);
}
