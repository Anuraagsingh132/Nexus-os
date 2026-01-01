package com.nexusos.api.workspace.security;

import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.workspace.domain.Membership;
import com.nexusos.api.workspace.domain.Role;
import com.nexusos.api.workspace.repository.MembershipRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component("workspaceSecurity")
public class WorkspaceSecurity {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public WorkspaceSecurity(MembershipRepository membershipRepository, UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public boolean hasRole(UUID workspaceId, String... allowedRoles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        Optional<Membership> membershipOpt = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userOpt.get().getId());
        if (membershipOpt.isEmpty()) {
            return false;
        }

        Role userRole = membershipOpt.get().getRole();
        
        // Owner has implicitly all permissions.
        if (userRole == Role.OWNER) {
            return true;
        }

        List<String> allowed = List.of(allowedRoles);
        return allowed.contains(userRole.name());
    }

    public boolean isMember(UUID workspaceId) {
        return hasRole(workspaceId, "OWNER", "ADMIN", "MANAGER", "MEMBER", "GUEST");
    }

    public boolean isContributor(UUID workspaceId) {
        return hasRole(workspaceId, "OWNER", "ADMIN", "MANAGER", "MEMBER");
    }
}
