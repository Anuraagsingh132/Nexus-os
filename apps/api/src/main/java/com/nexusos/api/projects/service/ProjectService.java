package com.nexusos.api.projects.service;

import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.domain.Task;
import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository, WorkspaceRepository workspaceRepository, UserRepository userRepository, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public Task reassignTask(UUID workspaceId, UUID taskId, UUID assigneeUserId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
        if (!task.getProject().getWorkspace().getId().equals(workspaceId)) {
            throw new IllegalArgumentException("Task does not belong to the specified workspace");
        }

        User assignee = null;
        if (assigneeUserId != null) {
            assignee = userRepository.findById(assigneeUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        
        task.setAssignee(assignee);
        task = taskRepository.save(task);

        messagingTemplate.convertAndSend(
            "/topic/workspaces/" + workspaceId + "/projects/" + task.getProject().getId() + "/tasks",
            task
        );

        return task;
    }

    @Transactional
    public List<Task> bulkUpdateTaskStatus(UUID workspaceId, UUID projectId, String fromStatus, String toStatus) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            
        if (!project.getWorkspace().getId().equals(workspaceId)) {
            throw new IllegalArgumentException("Project does not belong to the specified workspace");
        }

        List<Task> tasks = taskRepository.findByProjectIdOrderByPositionAsc(projectId);
        List<Task> updatedTasks = new java.util.ArrayList<>();
        
        for (Task task : tasks) {
            if (task.getStatus().equals(fromStatus)) {
                task.setStatus(toStatus);
                updatedTasks.add(taskRepository.save(task));
            }
        }
        
        if (!updatedTasks.isEmpty()) {
            messagingTemplate.convertAndSend(
                "/topic/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks",
                taskRepository.findByProjectIdOrderByPositionAsc(projectId)
            );
        }
        
        return updatedTasks;
    }
}
