package com.nexusos.api.workspace.controller;

import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.security.CustomUserDetails;
import com.nexusos.api.workspace.dto.CreateWorkspaceRequest;
import com.nexusos.api.workspace.dto.WorkspaceDto;
import com.nexusos.api.workspace.service.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceDto>> getWorkspaces(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(workspaceService.getUserWorkspaces(userDetails.getUser().getId()));
    }

    @PostMapping
    public ResponseEntity<WorkspaceDto> createWorkspace(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.ok(workspaceService.createWorkspace(userDetails.getUser().getId(), request));
    }
}
