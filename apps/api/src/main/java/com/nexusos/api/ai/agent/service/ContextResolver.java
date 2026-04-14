package com.nexusos.api.ai.agent.service;

import com.nexusos.api.chat.domain.Channel;
import com.nexusos.api.chat.repository.ChannelRepository;
import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.repository.DocumentRepository;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.workspace.domain.Membership;
import com.nexusos.api.workspace.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContextResolver {

    private final ProjectRepository projectRepository;
    private final MembershipRepository membershipRepository;
    private final DocumentRepository documentRepository;
    private final ChannelRepository channelRepository;

    public ContextResolver(ProjectRepository projectRepository, 
                           MembershipRepository membershipRepository, 
                           DocumentRepository documentRepository, 
                           ChannelRepository channelRepository) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.documentRepository = documentRepository;
        this.channelRepository = channelRepository;
    }

    public Optional<Project> resolveProject(UUID workspaceId, String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        List<Project> projects = projectRepository.findByWorkspaceId(workspaceId);
        return projects.stream()
                .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()) || 
                             query.toLowerCase().contains(p.getName().toLowerCase()))
                .findFirst();
    }

    public Optional<User> resolveUser(UUID workspaceId, String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        List<Membership> memberships = membershipRepository.findByWorkspaceId(workspaceId);
        return memberships.stream()
                .map(Membership::getUser)
                .filter(u -> u.getFullName().toLowerCase().contains(query.toLowerCase()) || 
                             u.getEmail().toLowerCase().contains(query.toLowerCase()) ||
                             query.toLowerCase().contains(u.getFullName().toLowerCase()))
                .findFirst();
    }

    public Optional<Document> resolveDocument(UUID workspaceId, String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        List<Document> documents = documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        return documents.stream()
                .filter(d -> d.getTitle().toLowerCase().contains(query.toLowerCase()) || 
                             query.toLowerCase().contains(d.getTitle().toLowerCase()))
                .findFirst();
    }

    public Optional<Channel> resolveChannel(UUID workspaceId, String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        List<Channel> channels = channelRepository.findByWorkspaceId(workspaceId);
        return channels.stream()
                .filter(c -> c.getName().toLowerCase().contains(query.toLowerCase()) || 
                             query.toLowerCase().contains(c.getName().toLowerCase()))
                .findFirst();
    }

    public String buildWorkspaceContextPrompt(UUID workspaceId, UUID sourceChannelId) {
        StringBuilder prompt = new StringBuilder("Workspace Context:\n");

        List<Project> projects = projectRepository.findByWorkspaceId(workspaceId);
        if (!projects.isEmpty()) {
            String projectNames = projects.stream()
                    .limit(10)
                    .map(Project::getName)
                    .collect(Collectors.joining(", "));
            prompt.append("- Available Projects: ").append(projectNames).append("\n");
        }

        List<Channel> channels = channelRepository.findByWorkspaceId(workspaceId);
        if (!channels.isEmpty()) {
            String channelNames = channels.stream()
                    .limit(10)
                    .map(Channel::getName)
                    .collect(Collectors.joining(", "));
            prompt.append("- Available Channels: ").append(channelNames).append("\n");
            
            if (sourceChannelId != null) {
                channels.stream()
                        .filter(c -> c.getId().equals(sourceChannelId))
                        .findFirst()
                        .ifPresent(c -> prompt.append("- Current Channel: ").append(c.getName()).append("\n"));
            }
        }

        List<Document> documents = documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        if (!documents.isEmpty()) {
            String docTitles = documents.stream()
                    .limit(10)
                    .map(Document::getTitle)
                    .collect(Collectors.joining(", "));
            prompt.append("- Available Documents: ").append(docTitles).append("\n");
        }

        List<Membership> memberships = membershipRepository.findByWorkspaceId(workspaceId);
        if (!memberships.isEmpty()) {
            String memberNames = memberships.stream()
                    .limit(15)
                    .map(m -> m.getUser().getFullName())
                    .collect(Collectors.joining(", "));
            prompt.append("- Workspace Members: ").append(memberNames).append("\n");
        }

        return prompt.toString();
    }
}
