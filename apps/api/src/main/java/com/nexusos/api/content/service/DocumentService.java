package com.nexusos.api.content.service;

import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.repository.DocumentRepository;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import com.nexusos.api.ai.service.DocumentIngestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DocumentIngestionService documentIngestionService;

    public DocumentService(DocumentRepository documentRepository, WorkspaceRepository workspaceRepository, DocumentIngestionService documentIngestionService) {
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.documentIngestionService = documentIngestionService;
    }

    @Transactional(readOnly = true)
    public List<Document> listDocuments(UUID workspaceId) {
        return documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
    }

    @Transactional(readOnly = true)
    public Document getDocument(UUID workspaceId, UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new java.util.NoSuchElementException("Document not found"));
        if (!document.getWorkspace().getId().equals(workspaceId)) {
            throw new java.util.NoSuchElementException("Document not found in this workspace");
        }
        return document;
    }

    @Transactional
    public Document createDocument(UUID workspaceId, String title, String content) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        Document document = new Document(workspace, title, content);
        Document saved = documentRepository.save(document);
        triggerIngestionAfterCommit(saved.getContent(), workspaceId, saved.getId(), saved.getTitle());
        return saved;
    }
    
    @Transactional
    public Document updateDocument(UUID workspaceId, UUID documentId, String title, String content) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (!document.getWorkspace().getId().equals(workspaceId)) {
            throw new java.util.NoSuchElementException("Document not found in this workspace");
        }
        if (title != null) document.setTitle(title);
        if (content != null) document.setContent(content);
        Document saved = documentRepository.save(document);
        triggerIngestionAfterCommit(saved.getContent(), workspaceId, saved.getId(), saved.getTitle());
        return saved;
    }

    private void triggerIngestionAfterCommit(String text, UUID workspaceId, UUID documentId, String title) {
        if (text == null || text.isBlank()) return;
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    documentIngestionService.ingestText(text, workspaceId, documentId, title);
                }
            });
        } else {
            documentIngestionService.ingestText(text, workspaceId, documentId, title);
        }
    }
}
