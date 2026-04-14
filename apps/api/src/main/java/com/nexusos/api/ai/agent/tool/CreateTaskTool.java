package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.domain.Task;
import com.nexusos.api.projects.service.ProjectService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CreateTaskTool implements AgentTool {

    private final ProjectService projectService;
    private final ContextResolver contextResolver;

    public CreateTaskTool(ProjectService projectService, ContextResolver contextResolver) {
        this.projectService = projectService;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getName() {
        return "create_task";
    }

    @Override
    public String getDescription() {
        return "Creates a new task in a project. Requires a project name or ID and a task title.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "project_name_or_id", Map.of("type", "string", "description", "Name or UUID of the project"),
                "title", Map.of("type", "string", "description", "Title of the task"),
                "description", Map.of("type", "string", "description", "Description of the task"),
                "status", Map.of("type", "string", "description", "Initial status (TODO, IN_PROGRESS, DONE). Defaults to TODO")
            ),
            "required", java.util.List.of("project_name_or_id", "title")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String projectRef = (String) arguments.get("project_name_or_id");
        String title = (String) arguments.get("title");
        String description = (String) arguments.getOrDefault("description", "");
        String status = (String) arguments.getOrDefault("status", "TODO");

        // Resolve project by UUID or fuzzy name match
        UUID projectId = null;
        try {
            projectId = UUID.fromString(projectRef);
        } catch (IllegalArgumentException e) {
            Optional<Project> projectOpt = contextResolver.resolveProject(workspaceId, projectRef);
            if (projectOpt.isPresent()) {
                projectId = projectOpt.get().getId();
            }
        }

        if (projectId == null) {
            return ToolResult.builder().success(false)
                    .errorMessage("Project not found: " + projectRef)
                    .summary("Failed to create task — project not found.").build();
        }

        try {
            Task task = projectService.createTask(workspaceId, projectId, title, description, status, 0);
            return ToolResult.builder()
                    .success(true)
                    .summary("Task '" + title + "' created in project.")
                    .data(Map.of("taskId", task.getId().toString(), "title", task.getTitle()))
                    .build();
        } catch (Exception e) {
            return ToolResult.builder().success(false)
                    .errorMessage(e.getMessage())
                    .summary("Failed to create task: " + e.getMessage()).build();
        }
    }
}
