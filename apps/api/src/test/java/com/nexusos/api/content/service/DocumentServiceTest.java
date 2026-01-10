package com.nexusos.api.content.service;

import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.repository.DocumentRepository;
import com.nexusos.api.workspace.domain.Organization;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.OrganizationRepository;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.nexusos.api.search.repository.DocumentSearchRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DocumentServiceTest {

    @MockBean
    private DocumentSearchRepository documentSearchRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void testCreateAndListDocuments() {
        Organization org = organizationRepository.save(new Organization("Doc Test Org", "doc-test-org"));
        Workspace workspace = workspaceRepository.save(new Workspace(org, "Doc Test WS", "doc-test-ws"));

        Document doc1 = documentService.createDocument(workspace.getId(), "Test Document 1", "Content 1");
        Document doc2 = documentService.createDocument(workspace.getId(), "Test Document 2", "Content 2");

        assertNotNull(doc1.getId());
        assertNotNull(doc2.getId());
        assertEquals("Test Document 1", doc1.getTitle());

        List<Document> docs = documentService.listDocuments(workspace.getId());
        assertEquals(2, docs.size());
    }

    @Test
    void testUpdateDocument() {
        Organization org = organizationRepository.save(new Organization("Update Test Org", "update-test-org"));
        Workspace workspace = workspaceRepository.save(new Workspace(org, "Update Test WS", "update-test-ws"));

        Document doc = documentService.createDocument(workspace.getId(), "Original Title", "Original Content");
        Document updated = documentService.updateDocument(workspace.getId(), doc.getId(), "Updated Title", null);

        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Original Content", updated.getContent());
    }

    @Test
    void testUpdateDocumentRejectsWrongWorkspace() {
        Organization org = organizationRepository.save(new Organization("IDOR Test Org", "idor-test-org"));
        Workspace workspace1 = workspaceRepository.save(new Workspace(org, "IDOR WS 1", "idor-ws-1"));
        Workspace workspace2 = workspaceRepository.save(new Workspace(org, "IDOR WS 2", "idor-ws-2"));

        Document doc = documentService.createDocument(workspace2.getId(), "Private", "Workspace 2 content");

        assertThrows(java.util.NoSuchElementException.class, () ->
            documentService.updateDocument(workspace1.getId(), doc.getId(), "Leaked", null)
        );
    }

    @Test
    void testCreateDocument_InvalidWorkspace() {
        assertThrows(IllegalArgumentException.class, () -> {
            documentService.createDocument(java.util.UUID.randomUUID(), "Title", "Content");
        });
    }
}
