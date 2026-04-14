package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.service.DocumentService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class AppendToDocumentTool implements AgentTool {

    private final DocumentService documentService;
    private final ContextResolver contextResolver;

    public AppendToDocumentTool(DocumentService documentService, ContextResolver contextResolver) {
        this.documentService = documentService;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getName() {
        return "append_to_document";
    }

    @Override
    public String getDescription() {
        return "Appends text content to an existing document identified by title or ID.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "document_title_or_id", Map.of("type", "string", "description", "Title or UUID of the document"),
                "content", Map.of("type", "string", "description", "Text to append")
            ),
            "required", java.util.List.of("document_title_or_id", "content")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String docRef = (String) arguments.get("document_title_or_id");
        String content = (String) arguments.get("content");

        Document document = null;
        try {
            UUID docId = UUID.fromString(docRef);
            document = documentService.getDocument(workspaceId, docId);
        } catch (IllegalArgumentException e) {
            Optional<Document> docOpt = contextResolver.resolveDocument(workspaceId, docRef);
            document = docOpt.orElse(null);
        } catch (Exception e) {
            // getDocument may throw NoSuchElementException
        }

        if (document == null) {
            return ToolResult.builder().success(false)
                    .errorMessage("Document not found: " + docRef)
                    .summary("Failed to append — document not found.").build();
        }

        try {
            String existingContent = document.getContent() != null ? document.getContent() : "";
            String updatedContent = existingContent + "\n" + content;
            documentService.updateDocument(workspaceId, document.getId(), null, updatedContent);
            return ToolResult.builder()
                    .success(true)
                    .summary("Content appended to document '" + document.getTitle() + "'.")
                    .build();
        } catch (Exception e) {
            return ToolResult.builder().success(false)
                    .errorMessage(e.getMessage())
                    .summary("Failed to append to document: " + e.getMessage()).build();
        }
    }
}
