package com.nexusos.api.documents;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import com.nexusos.api.ai.service.DocumentIngestionService;

@RestController
@RequestMapping("/api/v1/internal/documents")
public class InternalDocumentController {

    private final JdbcTemplate jdbcTemplate;
    private final DocumentIngestionService documentIngestionService;

    public InternalDocumentController(JdbcTemplate jdbcTemplate, DocumentIngestionService documentIngestionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.documentIngestionService = documentIngestionService;
    }

    @GetMapping(value = "/{id}/yjs", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getYjsState(@PathVariable String id) {
        UUID docId;
        try {
            docId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        String query = "SELECT yjs_state FROM documents WHERE id = ?";
        byte[] state = jdbcTemplate.query(
                query,
                rs -> {
                    if (rs.next()) {
                        return rs.getBytes("yjs_state");
                    }
                    return null;
                },
                docId
        );

        if (state == null || state.length == 0) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(state);
    }

    @PutMapping(value = "/{id}/yjs", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> updateYjsState(@PathVariable String id, @RequestBody byte[] yjsState) {
        UUID docId;
        try {
            docId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        String updateQuery = "UPDATE documents SET yjs_state = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        int updated = jdbcTemplate.update(updateQuery, yjsState, docId);
        
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok().build();
    }

    @PatchMapping(value = "/{id}/content", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateContent(@PathVariable String id, @RequestBody java.util.Map<String, String> body) {
        UUID docId;
        try {
            docId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        String content = body.get("content");
        if (content == null) {
            return ResponseEntity.badRequest().build();
        }

        String selectQuery = "SELECT workspace_id, title FROM documents WHERE id = ?";
        java.util.Map<String, Object> docInfo;
        try {
            docInfo = jdbcTemplate.queryForMap(selectQuery, docId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }

        UUID workspaceId = (UUID) docInfo.get("workspace_id");
        String title = (String) docInfo.get("title");

        String updateQuery = "UPDATE documents SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        int updated = jdbcTemplate.update(updateQuery, content, docId);
        
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        
        if (content != null && !content.isBlank() && workspaceId != null) {
            try {
                documentIngestionService.ingestText(content, workspaceId, docId, title != null ? title : "Untitled Document");
            } catch (Exception e) {
                // Log and swallow so content patch doesn't fail
            }
        }

        return ResponseEntity.ok().build();
    }
}
