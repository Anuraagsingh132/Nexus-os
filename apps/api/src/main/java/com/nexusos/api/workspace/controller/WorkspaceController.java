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

import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.content.repository.DocumentRepository;
import com.nexusos.api.workspace.repository.MembershipRepository;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.admin.ActivityDataPointDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public WorkspaceController(WorkspaceService workspaceService,
                               ProjectRepository projectRepository,
                               TaskRepository taskRepository,
                               DocumentRepository documentRepository,
                               MembershipRepository membershipRepository,
                               UserRepository userRepository) {
        this.workspaceService = workspaceService;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceDto>> getWorkspaces(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(workspaceService.getUserWorkspaces(userDetails.getUser().getId()));
    }

    @PostMapping
    public ResponseEntity<WorkspaceDto> createWorkspace(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.ok(workspaceService.createWorkspace(userDetails.getUser().getId(), request));
    }

    @GetMapping("/{workspaceId}/stats")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<Map<String, Object>> getWorkspaceStats(@PathVariable UUID workspaceId) {
        long activeProjects = projectRepository.findByWorkspaceId(workspaceId).size();
        long totalTasks = taskRepository.countByProjectWorkspaceId(workspaceId);
        long teamMembers = membershipRepository.findByWorkspaceId(workspaceId).size();
        long documents = documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).size();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeProjects", activeProjects);
        stats.put("tasksCompleted", totalTasks);
        stats.put("teamMembers", teamMembers);
        stats.put("documents", documents);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{workspaceId}/activity")
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<List<ActivityDataPointDto>> getWorkspaceActivity(@PathVariable UUID workspaceId) {
        // Return workspace member count metrics
        long count = membershipRepository.findByWorkspaceId(workspaceId).size();
        List<ActivityDataPointDto> activity = List.of(
            new ActivityDataPointDto("Members", count)
        );
        return ResponseEntity.ok(activity);
    }
}
