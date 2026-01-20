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

import com.nexusos.api.workspace.domain.PendingInvite;
import com.nexusos.api.workspace.repository.PendingInviteRepository;

import java.util.Optional;

@Service
public class WorkspaceInviteService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final PendingInviteRepository pendingInviteRepository;
    private final NotificationService notificationService;

    public WorkspaceInviteService(UserRepository userRepository,
                                  WorkspaceRepository workspaceRepository,
                                  MembershipRepository membershipRepository,
                                  PendingInviteRepository pendingInviteRepository,
                                  NotificationService notificationService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.pendingInviteRepository = pendingInviteRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void inviteUserToWorkspace(UUID workspaceId, String email) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (membershipRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId()).isPresent()) {
                throw new IllegalArgumentException("User is already a member of this workspace");
            }
            Membership membership = new Membership(user, workspace, Role.MEMBER);
            membershipRepository.save(membership);

            String title = "Workspace Invitation";
            String message = "You have been invited to join the workspace: " + workspace.getName();
            notificationService.createAndSendNotification(user.getId(), title, message);
        } else {
            if (pendingInviteRepository.findByWorkspaceIdAndEmail(workspaceId, email).isEmpty()) {
                pendingInviteRepository.save(new PendingInvite(workspace, email));
            }
        }
    }
}
