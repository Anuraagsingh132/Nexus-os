package com.nexusos.api.content.repository;

import com.nexusos.api.content.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT d FROM Document d WHERE d.workspace.id = :workspaceId AND d.deletedAt IS NULL ORDER BY d.updatedAt DESC")
    List<Document> findByWorkspaceIdOrderByUpdatedAtDesc(UUID workspaceId);
    
    @org.springframework.data.jpa.repository.Query("SELECT d FROM Document d WHERE d.workspace.id = :workspaceId AND d.deletedAt IS NULL AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Document> searchByTitleOrContent(UUID workspaceId, String query);
}
