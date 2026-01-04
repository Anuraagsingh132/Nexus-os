package com.nexusos.api.search.service;

import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final DocumentRepository documentRepository;

    public SearchService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<Document> search(UUID workspaceId, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return documentRepository.searchByTitleOrContent(workspaceId, query);
    }
}
