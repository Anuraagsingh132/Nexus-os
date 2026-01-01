package com.nexusos.api.workspace.service;

import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.workspace.domain.Membership;
import com.nexusos.api.workspace.domain.Organization;
import com.nexusos.api.workspace.domain.Role;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.dto.CreateWorkspaceRequest;
import com.nexusos.api.workspace.dto.WorkspaceDto;
import com.nexusos.api.workspace.repository.MembershipRepository;
import com.nexusos.api.workspace.repository.OrganizationRepository;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final com.nexusos.api.notifications.service.NotificationService notificationService;

    public WorkspaceService(WorkspaceRepository workspaceRepository, MembershipRepository membershipRepository, OrganizationRepository organizationRepository, UserRepository userRepository, com.nexusos.api.notifications.service.NotificationService notificationService) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> getUserWorkspaces(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(m -> {
                    Workspace w = m.getWorkspace();
                    return new WorkspaceDto(w.getId(), w.getName(), w.getSlug());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceDto createWorkspace(UUID userId, CreateWorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Use the first organization or create a personal one
        Organization org = organizationRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Organization newOrg = new Organization(user.getFullName() + " Org", user.getFullName().toLowerCase().replace(" ", "-") + "-org");
                    return organizationRepository.save(newOrg);
                });

        String slug = request.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        Workspace workspace = new Workspace(org, request.getName(), slug);
        workspace = workspaceRepository.save(workspace);

        Membership membership = new Membership(user, workspace, Role.OWNER);
        membershipRepository.save(membership);

        notificationService.createAndSendNotification(
            user.getId(), 
            "Workspace Created", 
            "You successfully created the workspace: " + workspace.getName()
        );

        return new WorkspaceDto(workspace.getId(), workspace.getName(), workspace.getSlug());
    }
}
