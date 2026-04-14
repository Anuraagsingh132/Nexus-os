package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.ai.service.AiService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SearchWorkspaceKnowledgeTool implements AgentTool {

    private final AiService aiService;

    public SearchWorkspaceKnowledgeTool(AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public String getName() {
        return "search_workspace_knowledge";
    }

    @Override
    public String getDescription() {
        return "Searches the workspace knowledge base (documents, files) using semantic/RAG search and returns relevant results.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string", "description", "The search query")
            ),
            "required", java.util.List.of("query")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String query = (String) arguments.get("query");

        try {
            AiService.AiResult result = aiService.getAiResponse(workspaceId, query);
            String citations = result.citations().stream()
                    .map(c -> "- " + c.getOrDefault("title", "Untitled"))
                    .collect(Collectors.joining("\n"));

            return ToolResult.builder()
                    .success(true)
                    .summary("Knowledge search completed for: " + query)
                    .data(Map.of("answer", result.answer(), "citations", citations))
                    .build();
        } catch (Exception e) {
            return ToolResult.builder().success(false)
                    .errorMessage(e.getMessage())
                    .summary("Knowledge search failed: " + e.getMessage()).build();
        }
    }
}
