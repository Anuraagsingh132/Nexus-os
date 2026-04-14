package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.projects.domain.Task;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.projects.service.ProjectService;
import com.nexusos.api.identity.domain.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Component
public class ReassignTaskTool implements AgentTool {

    private final ProjectService projectService;
    private final ContextResolver contextResolver;
    private final TaskRepository taskRepository;

    public ReassignTaskTool(ProjectService projectService, ContextResolver contextResolver, TaskRepository taskRepository) {
        this.projectService = projectService;
        this.contextResolver = contextResolver;
        this.taskRepository = taskRepository;
    }

    @Override
    public String getName() {
        return "reassign_task";
    }

    @Override
    public String getDescription() {
        return "Reassigns a task to a new user.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "task_title_or_id", Map.of("type", "string", "description", "Title or ID of the task"),
                "new_assignee_name", Map.of("type", "string", "description", "Name of the new assignee")
            ),
            "required", java.util.List.of("task_title_or_id", "new_assignee_name")
        );
    }

    @Override
    public boolean isHighImpact() {
        return true;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String taskTitleOrId = (String) arguments.get("task_title_or_id");
        String newAssigneeName = (String) arguments.get("new_assignee_name");

        Optional<User> userOpt = contextResolver.resolveUser(workspaceId, newAssigneeName);
        if (userOpt.isEmpty()) {
            return ToolResult.builder().success(false).summary("User not found: " + newAssigneeName).build();
        }
        User assignee = userOpt.get();

        UUID taskId = null;
        try {
            taskId = UUID.fromString(taskTitleOrId);
        } catch (IllegalArgumentException e) {
            // High-performance indexed database lookup by title within workspace
            Optional<Task> taskOpt = taskRepository.findFirstByProjectWorkspaceIdAndTitleIgnoreCase(workspaceId, taskTitleOrId);
            if (taskOpt.isPresent()) {
                taskId = taskOpt.get().getId();
            }
        }

        if (taskId == null) {
            return ToolResult.builder().success(false).summary("Task not found: " + taskTitleOrId).build();
        }

        projectService.reassignTask(workspaceId, taskId, assignee.getId());

        return ToolResult.builder()
                .success(true)
                .summary("Task reassigned to " + assignee.getFullName())
                .build();
    }
}
