package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.workspace.service.WorkspaceInviteService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class DraftWorkspaceInviteTool implements AgentTool {

    private final WorkspaceInviteService workspaceInviteService;

    public DraftWorkspaceInviteTool(WorkspaceInviteService workspaceInviteService) {
        this.workspaceInviteService = workspaceInviteService;
    }

    @Override
    public String getName() {
        return "draft_workspace_invite";
    }

    @Override
    public String getDescription() {
        return "Drafts an invite for a user to join the workspace.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "email", Map.of("type", "string", "description", "Email address to invite")
            ),
            "required", java.util.List.of("email")
        );
    }

    @Override
    public boolean isHighImpact() {
        return true;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String email = (String) arguments.get("email");

        workspaceInviteService.inviteUserToWorkspace(workspaceId, email);

        return ToolResult.builder()
                .success(true)
                .summary("Successfully invited user to workspace: " + email)
                .build();
    }
}
