package com.nexusos.api.projects.service;

import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.domain.Task;
import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository, WorkspaceRepository workspaceRepository, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceRepository = workspaceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<Project> listProjects(UUID workspaceId) {
        return projectRepository.findByWorkspaceId(workspaceId);
    }

    @Transactional
    public Project createProject(UUID workspaceId, String name, String description) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        Project project = new Project(workspace, name, description);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Task> listTasks(UUID workspaceId, UUID projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getWorkspace().getId().equals(workspaceId)) {
            throw new IllegalArgumentException("Project does not belong to the specified workspace");
        }
        return taskRepository.findByProjectIdOrderByPositionAsc(projectId);
    }

    @Transactional
    public Task createTask(UUID workspaceId, UUID projectId, String title, String description, String status, Integer position) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getWorkspace().getId().equals(workspaceId)) {
            throw new IllegalArgumentException("Project does not belong to the specified workspace");
        }
        Task task = new Task(project, title, description, status, position);
        return taskRepository.save(task);
    }

    @Transactional
    public Task moveTask(UUID workspaceId, UUID projectId, UUID taskId, String newStatus, Integer newPosition) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getWorkspace().getId().equals(workspaceId)) {
            throw new IllegalArgumentException("Project does not belong to the specified workspace");
        }
        
        Task taskToMove = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
        if (!taskToMove.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Task does not belong to the specified project");
        }

        // Get all tasks in the target status, ordered by position
        List<Task> targetTasks = taskRepository.findByProjectIdOrderByPositionAsc(projectId)
            .stream()
            .filter(t -> t.getStatus().equals(newStatus) && !t.getId().equals(taskId))
            .collect(java.util.stream.Collectors.toList());

        // Update the task's status
        taskToMove.setStatus(newStatus);

        // Insert into the new position
        int boundedPosition = Math.max(0, Math.min(newPosition, targetTasks.size()));
        targetTasks.add(boundedPosition, taskToMove);

        // Reassign positions sequentially to avoid gaps or duplicates
        for (int i = 0; i < targetTasks.size(); i++) {
            targetTasks.get(i).setPosition(i);
            taskRepository.save(targetTasks.get(i));
        }
        
        List<Task> updatedTasks = taskRepository.findByProjectIdOrderByPositionAsc(projectId);
        messagingTemplate.convertAndSend(
                "/topic/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks",
                updatedTasks
        );

        return taskToMove;
    }
}
