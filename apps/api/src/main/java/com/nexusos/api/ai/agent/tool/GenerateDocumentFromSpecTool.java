package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.ai.service.AiService;
import com.nexusos.api.content.service.DocumentService;
import com.nexusos.api.content.domain.Document;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class GenerateDocumentFromSpecTool implements AgentTool {

    private final AiService aiService;
    private final DocumentService documentService;

    public GenerateDocumentFromSpecTool(AiService aiService, DocumentService documentService) {
        this.aiService = aiService;
        this.documentService = documentService;
    }

    @Override
    public String getName() {
        return "generate_document_from_spec";
    }

    @Override
    public String getDescription() {
        return "Generates a document from a spec query using AI.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "spec_query", Map.of("type", "string", "description", "The query to search the knowledge base"),
                "instruction", Map.of("type", "string", "description", "Instructions on how to generate the document")
            ),
            "required", java.util.List.of("spec_query", "instruction")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String specQuery = (String) arguments.get("spec_query");
        String instruction = (String) arguments.get("instruction");

        String fullQuery = "Based on the following spec: " + specQuery + "\nInstructions: " + instruction;
        AiService.AiResult result = aiService.getAiResponse(workspaceId, fullQuery);

        Document doc = documentService.createDocument(workspaceId, "Generated Document", result.answer());

        return ToolResult.builder()
                .success(true)
                .summary("Generated document with ID: " + doc.getId())
                .build();
    }
}
