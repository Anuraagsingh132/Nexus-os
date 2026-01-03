package com.nexusos.api.projects.controller;

import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.domain.Task;
import com.nexusos.api.projects.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public List<Project> listProjects(@PathVariable UUID workspaceId) {
        return projectService.listProjects(workspaceId);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public Project getProject(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
        return projectService.listProjects(workspaceId).stream().filter(p -> p.getId().equals(projectId)).findFirst().orElseThrow();
    }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole(#workspaceId, 'OWNER', 'ADMIN', 'MANAGER')")
    public Project createProject(@PathVariable UUID workspaceId, @RequestBody CreateProjectRequest request) {
        return projectService.createProject(workspaceId, request.name(), request.description());
    }

    @GetMapping("/{projectId}/tasks")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public List<Task> listTasks(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
        return projectService.listTasks(workspaceId, projectId);
    }

    @PostMapping("/{projectId}/tasks")
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    public Task createTask(@PathVariable UUID workspaceId, @PathVariable UUID projectId, @RequestBody CreateTaskRequest request) {
        return projectService.createTask(workspaceId, projectId, request.title(), request.description(), request.status(), request.position());
    }

    @PutMapping("/{projectId}/tasks/{taskId}/move")
    @PreAuthorize("@workspaceSecurity.isContributor(#workspaceId)")
    public Task moveTask(@PathVariable UUID workspaceId, @PathVariable UUID projectId, @PathVariable UUID taskId, @RequestBody MoveTaskRequest request) {
        return projectService.moveTask(workspaceId, projectId, taskId, request.status(), request.position());
    }
}

record CreateProjectRequest(String name, String description) {}
record CreateTaskRequest(String title, String description, String status, Integer position) {}
record MoveTaskRequest(String status, Integer position) {}
