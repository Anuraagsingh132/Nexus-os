package com.nexusos.api.files.controller;

import com.nexusos.api.files.domain.FileMetadata;
import com.nexusos.api.files.repository.FileMetadataRepository;
import com.nexusos.api.files.service.MinioService;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final MinioService minioService;
    private final WorkspaceRepository workspaceRepository;
    private final com.nexusos.api.ai.service.DocumentIngestionService documentIngestionService;
    private final com.nexusos.api.workspace.repository.MembershipRepository membershipRepository;
    private final com.nexusos.api.notifications.service.NotificationService notificationService;

    public FileController(FileMetadataRepository fileMetadataRepository, MinioService minioService, WorkspaceRepository workspaceRepository, com.nexusos.api.ai.service.DocumentIngestionService documentIngestionService, com.nexusos.api.workspace.repository.MembershipRepository membershipRepository, com.nexusos.api.notifications.service.NotificationService notificationService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.minioService = minioService;
        this.workspaceRepository = workspaceRepository;
        this.documentIngestionService = documentIngestionService;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<List<FileMetadata>> getFiles(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(fileMetadataRepository.findByWorkspaceId(workspaceId));
    }

    @PostMapping("/upload")
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> uploadFile(@PathVariable UUID workspaceId, @RequestParam("file") MultipartFile file) {
        try {
            Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
            
            String objectKey = minioService.uploadFile(file, workspaceId);
            
            FileMetadata fileMetadata = new FileMetadata(workspace, file.getOriginalFilename(), objectKey, file.getSize(), file.getContentType());
            fileMetadata = fileMetadataRepository.save(fileMetadata);
            
            // Asynchronously ingest document into VectorStore after transaction commit
            final UUID docId = fileMetadata.getId();
            final String title = fileMetadata.getName();
            final String contentType = file.getContentType();
            final byte[] fileBytes = file.getBytes();

            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (contentType != null && contentType.equals("application/pdf")) {
                            try { documentIngestionService.ingestPdf(fileBytes, workspaceId, docId, title); } 
                            catch (Exception e) { log.error("PDF Ingestion failed for file '{}': {}", title, e.getMessage(), e); }
                        } else if (contentType != null && contentType.startsWith("text/")) {
                            String text = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
                            try { documentIngestionService.ingestText(text, workspaceId, docId, title); } 
                            catch (Exception e) { log.error("Text Ingestion failed for file '{}': {}", title, e.getMessage(), e); }
                        }
                    }
                }
            );
            
            // Notify workspace members
            List<com.nexusos.api.workspace.domain.Membership> members = membershipRepository.findByWorkspaceId(workspaceId);
            for (com.nexusos.api.workspace.domain.Membership member : members) {
                notificationService.createAndSendNotification(
                        member.getUser().getId(),
                        "New File Uploaded",
                        "File " + file.getOriginalFilename() + " was uploaded to workspace " + workspace.getName()
                );
            }
            
            return ResponseEntity.ok(fileMetadata);
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "File upload failed");
            body.put("message", e.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }

    @GetMapping("/{fileId}/download")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<?> getDownloadUrl(@PathVariable UUID workspaceId, @PathVariable UUID fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId).orElseThrow();
            if (!metadata.getWorkspace().getId().equals(workspaceId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            String url = minioService.getPresignedDownloadUrl(metadata.getObjectKey());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Download URL generation failed for fileId {}: {}", fileId, e.getMessage(), e);
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "Download URL generation failed");
            body.put("message", e.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }

    @PostMapping("/{fileId}/retry-ingestion")
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> retryIngestion(@PathVariable UUID workspaceId, @PathVariable UUID fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId).orElseThrow();
            if (!metadata.getWorkspace().getId().equals(workspaceId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            if (!"FAILED".equals(metadata.getIngestionStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only FAILED ingestions can be retried"));
            }
            
            metadata.setIngestionStatus("PENDING");
            metadata.setIngestionError(null);
            fileMetadataRepository.save(metadata);
            
            final byte[] bytes = minioService.downloadFile(metadata.getObjectKey());
            final String contentType = metadata.getContentType();
            final String name = metadata.getName();

            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (contentType != null && contentType.equals("application/pdf")) {
                            try { documentIngestionService.ingestPdf(bytes, workspaceId, fileId, name); }
                            catch (Exception e) { log.error("PDF Ingestion retry failed: {}", e.getMessage()); }
                        } else if (contentType != null && contentType.startsWith("text/")) {
                            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                            try { documentIngestionService.ingestText(text, workspaceId, fileId, name); }
                            catch (Exception e) { log.error("Text Ingestion retry failed: {}", e.getMessage()); }
                        }
                    }
                }
            );
            
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Retry ingestion failed: {}", e.getMessage(), e);
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "Retry ingestion failed");
            body.put("message", e.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }

    @DeleteMapping("/{fileId}")
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    public ResponseEntity<?> deleteFile(@PathVariable UUID workspaceId, @PathVariable UUID fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findById(fileId).orElseThrow();
            if (!metadata.getWorkspace().getId().equals(workspaceId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            minioService.deleteFile(metadata.getObjectKey());
            fileMetadataRepository.delete(metadata);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("File deletion failed for fileId {}: {}", fileId, e.getMessage(), e);
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "File deletion failed");
            body.put("message", e.getMessage());
            return ResponseEntity.status(500).body(body);
        }
    }
}
