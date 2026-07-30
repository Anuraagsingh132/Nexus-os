package com.nexusos.api.projects.repository;

import com.nexusos.api.projects.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByProjectIdOrderByPositionAsc(UUID projectId);

    java.util.Optional<Task> findFirstByProjectWorkspaceIdAndTitleIgnoreCase(UUID workspaceId, String title);

    long countByProjectWorkspaceId(UUID workspaceId);

    @Modifying
    @Query("DELETE FROM Task t WHERE t.project.id IN (SELECT p.id FROM Project p WHERE p.workspace.id = :workspaceId)")
    void deleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
