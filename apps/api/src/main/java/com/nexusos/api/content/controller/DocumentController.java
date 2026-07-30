package com.nexusos.api.content.controller;

import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.service.DocumentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public List<Document> listDocuments(@PathVariable UUID workspaceId) {
        return documentService.listDocuments(workspaceId);
    }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    public Document createDocument(@PathVariable UUID workspaceId, @RequestBody CreateDocumentRequest request) {
        return documentService.createDocument(workspaceId, request.title(), request.content());
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<Document> getDocument(@PathVariable UUID workspaceId, @PathVariable UUID documentId) {
        Document document = documentService.getDocument(workspaceId, documentId);
        return ResponseEntity.ok(document);
    }

    @RequestMapping(value = "/{documentId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    public Document updateDocument(@PathVariable UUID workspaceId, @PathVariable UUID documentId, @RequestBody UpdateDocumentRequest request) {
        return documentService.updateDocument(workspaceId, documentId, request.title(), request.content());
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID workspaceId, @PathVariable UUID documentId) {
        documentService.deleteDocument(workspaceId, documentId);
        return ResponseEntity.noContent().build();
    }
}

record CreateDocumentRequest(String title, String content) {}
record UpdateDocumentRequest(String title, String content) {}
