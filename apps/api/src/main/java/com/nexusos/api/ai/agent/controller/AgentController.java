package com.nexusos.api.ai.agent.controller;

import com.nexusos.api.ai.agent.dto.AgentSettingsDto;
import com.nexusos.api.ai.agent.service.AgentService;
import com.nexusos.api.ai.entity.AgentActivity;
import com.nexusos.api.ai.repository.AgentActivityRepository;
import com.nexusos.api.identity.security.CustomUserDetails;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/agent")
public class AgentController {

    private final AgentService agentService;
    private final AgentActivityRepository agentActivityRepository;

    public AgentController(AgentService agentService, AgentActivityRepository agentActivityRepository) {
        this.agentService = agentService;
        this.agentActivityRepository = agentActivityRepository;
    }

    @GetMapping("/activities")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<Page<AgentActivity>> getActivities(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<AgentActivity> page;
        if (status != null && !status.isBlank()) {
            page = agentActivityRepository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, status, pageable);
        } else {
            page = agentActivityRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable);
        }
        return ResponseEntity.ok(page);
    }

    @GetMapping("/settings")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<AgentSettingsDto> getSettings(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(agentService.getAgentSettings(workspaceId));
    }

    @PatchMapping("/settings")
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'ADMIN') or @workspaceSecurity.hasRole(#workspaceId, 'OWNER')")
    public ResponseEntity<AgentSettingsDto> updateSettings(
            @PathVariable UUID workspaceId,
            @RequestBody AgentSettingsDto settings) {
        return ResponseEntity.ok(agentService.updateAgentSettings(workspaceId, settings));
    }

    @PostMapping("/activities/{id}/confirm")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<Object> confirmActivity(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(agentService.confirmActivity(workspaceId, id, userDetails.getId()));
    }

    @PostMapping("/activities/{id}/cancel")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<Object> cancelActivity(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(agentService.cancelActivity(workspaceId, id, userDetails.getId()));
    }
}
