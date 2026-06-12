package com.nexusos.api.files.repository;

import com.nexusos.api.files.domain.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findByWorkspaceId(UUID workspaceId);

    List<FileMetadata> findByIngestionStatusAndUpdatedAtBefore(String status, Instant threshold);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileMetadata f WHERE f.deletedAt IS NULL")
    Long sumSizeBytes();
}

