package com.nexusos.api.workspace.controller;

import com.nexusos.api.workspace.service.WorkspaceInviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceInviteController {

    private final WorkspaceInviteService workspaceInviteService;

    public WorkspaceInviteController(WorkspaceInviteService workspaceInviteService) {
        this.workspaceInviteService = workspaceInviteService;
    }

    @PostMapping("/{workspaceId}/invites")
    @org.springframework.security.access.prepost.PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'OWNER', 'ADMIN')")
    public ResponseEntity<Void> inviteUser(@PathVariable UUID workspaceId, @RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        workspaceInviteService.inviteUserToWorkspace(workspaceId, email);
        return ResponseEntity.ok().build();
    }
}
