package com.nexusos.api.ai.repository;

import com.nexusos.api.ai.entity.AgentActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AgentActivityRepository extends JpaRepository<AgentActivity, UUID> {
    Page<AgentActivity> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);
    Page<AgentActivity> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, String status, Pageable pageable);
    java.util.List<AgentActivity> findByStatusAndCreatedAtBefore(String status, java.time.OffsetDateTime createdAt);
}
