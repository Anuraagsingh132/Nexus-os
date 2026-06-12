package com.nexusos.api.projects.repository;

import com.nexusos.api.projects.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByWorkspaceId(UUID workspaceId);

    @Modifying
    @Query("DELETE FROM Project p WHERE p.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
