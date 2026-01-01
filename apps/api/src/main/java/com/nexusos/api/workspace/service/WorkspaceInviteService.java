package com.nexusos.api.workspace.service;

import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.notifications.service.NotificationService;
import com.nexusos.api.workspace.domain.Membership;
import com.nexusos.api.workspace.domain.Role;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.MembershipRepository;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspaceInviteService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final NotificationService notificationService;

    public WorkspaceInviteService(UserRepository userRepository,
                                  WorkspaceRepository workspaceRepository,
                                  MembershipRepository membershipRepository,
                                  NotificationService notificationService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void inviteUserToWorkspace(UUID workspaceId, String email) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (membershipRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId()).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this workspace");
        }

        Membership membership = new Membership(user, workspace, Role.MEMBER);
        membershipRepository.save(membership);

        String title = "Workspace Invitation";
        String message = "You have been invited to join the workspace: " + workspace.getName();
        notificationService.createAndSendNotification(user.getId(), title, message);
    }
}
