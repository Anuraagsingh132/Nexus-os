package com.nexusos.api.ai.controller;

import com.nexusos.api.ai.service.AiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/query")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public AiResponse query(@PathVariable UUID workspaceId, @RequestBody AiRequest request) {
        AiService.AiResult result = aiService.getAiResponse(workspaceId, request.query());
        return new AiResponse(result.answer(), result.citations());
    }
}

record AiRequest(String query) {}
record AiResponse(String answer, java.util.List<java.util.Map<String, String>> citations) {}
