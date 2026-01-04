package com.nexusos.api.search.controller;

import com.nexusos.api.content.domain.Document;
import com.nexusos.api.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<List<Map<String, String>>> search(
            @PathVariable UUID workspaceId,
            @RequestParam("q") String query) {
        
        List<Document> results = searchService.search(workspaceId, query);
        
        // Map to a generic format similar to what DocumentSearch returned
        List<Map<String, String>> response = results.stream().map(doc -> Map.of(
            "id", doc.getId().toString(),
            "workspaceId", workspaceId.toString(),
            "title", doc.getTitle(),
            "content", doc.getContent() != null ? doc.getContent() : ""
        )).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
}
