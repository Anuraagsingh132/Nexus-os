package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.projects.domain.Task;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.projects.service.ProjectService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class UpdateTaskStatusTool implements AgentTool {

    private final ProjectService projectService;
    private final TaskRepository taskRepository;

    public UpdateTaskStatusTool(ProjectService projectService, TaskRepository taskRepository) {
        this.projectService = projectService;
        this.taskRepository = taskRepository;
    }

    @Override
    public String getName() {
        return "update_task_status";
    }

    @Override
    public String getDescription() {
        return "Updates the status of an existing task (e.g., TODO -> IN_PROGRESS -> DONE).";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "task_title_or_id", Map.of("type", "string", "description", "Title or UUID of the task"),
                "status", Map.of("type", "string", "description", "New status (TODO, IN_PROGRESS, DONE)")
            ),
            "required", java.util.List.of("task_title_or_id", "status")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String taskRef = (String) arguments.get("task_title_or_id");
        String newStatus = (String) arguments.get("status");

        Task foundTask = null;
        try {
            UUID taskId = UUID.fromString(taskRef);
            foundTask = taskRepository.findById(taskId).orElse(null);
        } catch (IllegalArgumentException e) {
            // High-performance indexed database lookup by title within workspace
            Optional<Task> taskOpt = taskRepository.findFirstByProjectWorkspaceIdAndTitleIgnoreCase(workspaceId, taskRef);
            foundTask = taskOpt.orElse(null);
        }

        if (foundTask == null) {
            return ToolResult.builder().success(false)
                    .errorMessage("Task not found: " + taskRef)
                    .summary("Failed to update task — not found.").build();
        }

        // Verify workspace scope
        if (!foundTask.getProject().getWorkspace().getId().equals(workspaceId)) {
            return ToolResult.builder().success(false)
                    .errorMessage("Task does not belong to this workspace")
                    .summary("Failed to update task — access denied.").build();
        }

        try {
            projectService.moveTask(workspaceId, foundTask.getProject().getId(), foundTask.getId(), newStatus, foundTask.getPosition());
            return ToolResult.builder()
                    .success(true)
                    .summary("Task '" + foundTask.getTitle() + "' status updated to " + newStatus)
                    .build();
        } catch (Exception e) {
            return ToolResult.builder().success(false)
                    .errorMessage(e.getMessage())
                    .summary("Failed to update task: " + e.getMessage()).build();
        }
    }
}
