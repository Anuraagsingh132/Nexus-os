package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.service.ProjectService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Component
public class BulkUpdateTaskStatusTool implements AgentTool {

    private final ProjectService projectService;
    private final ContextResolver contextResolver;

    public BulkUpdateTaskStatusTool(ProjectService projectService, ContextResolver contextResolver) {
        this.projectService = projectService;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getName() {
        return "bulk_update_task_status";
    }

    @Override
    public String getDescription() {
        return "Bulk updates task statuses in a project.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "project_name_or_id", Map.of("type", "string", "description", "Project Name or ID"),
                "from_status", Map.of("type", "string", "description", "Current status of tasks"),
                "to_status", Map.of("type", "string", "description", "New status for tasks")
            ),
            "required", java.util.List.of("project_name_or_id", "from_status", "to_status")
        );
    }

    @Override
    public boolean isHighImpact() {
        return true;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String projectNameOrId = (String) arguments.get("project_name_or_id");
        String fromStatus = (String) arguments.get("from_status");
        String toStatus = (String) arguments.get("to_status");

        Optional<Project> projectOpt = contextResolver.resolveProject(workspaceId, projectNameOrId);
        if (projectOpt.isEmpty()) {
            return ToolResult.builder().success(false).summary("Project not found: " + projectNameOrId).build();
        }

        projectService.bulkUpdateTaskStatus(workspaceId, projectOpt.get().getId(), fromStatus, toStatus);

        return ToolResult.builder()
                .success(true)
                .summary("Successfully bulk updated task statuses.")
                .build();
    }
}
