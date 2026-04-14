package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.service.DocumentService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class CreateDocumentTool implements AgentTool {

    private final DocumentService documentService;

    public CreateDocumentTool(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public String getName() {
        return "create_document";
    }

    @Override
    public String getDescription() {
        return "Creates a new document in the workspace with an optional initial content body.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "title", Map.of("type", "string", "description", "Title of the document"),
                "content", Map.of("type", "string", "description", "Initial content of the document")
            ),
            "required", java.util.List.of("title")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String title = (String) arguments.get("title");
        String content = (String) arguments.getOrDefault("content", "");

        try {
            Document doc = documentService.createDocument(workspaceId, title, content);
            return ToolResult.builder()
                    .success(true)
                    .summary("Document '" + title + "' created successfully.")
                    .data(Map.of("documentId", doc.getId().toString(), "title", doc.getTitle()))
                    .build();
        } catch (Exception e) {
            return ToolResult.builder().success(false)
                    .errorMessage(e.getMessage())
                    .summary("Failed to create document: " + e.getMessage()).build();
        }
    }
}
