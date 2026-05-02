package com.nexusos.api.ai.agent;

import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.chat.domain.Channel;
import com.nexusos.api.chat.repository.ChannelRepository;
import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.repository.DocumentRepository;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.workspace.domain.Membership;
import com.nexusos.api.workspace.domain.Role;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ContextResolverTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ChannelRepository channelRepository;

    @InjectMocks
    private ContextResolver contextResolver;

    private UUID workspaceId;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        workspace = mock(Workspace.class);
    }

    @Test
    void testResolveProject_FuzzyMatch() {
        Project p1 = new Project(workspace, "Q3 Marketing Board", "Desc");
        Project p2 = new Project(workspace, "Development Sync", "Desc");
        when(projectRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(p1, p2));

        Optional<Project> result = contextResolver.resolveProject(workspaceId, "q3");
        assertTrue(result.isPresent());
        assertEquals("Q3 Marketing Board", result.get().getName());

        Optional<Project> result2 = contextResolver.resolveProject(workspaceId, "Dev");
        assertTrue(result2.isPresent());
        assertEquals("Development Sync", result2.get().getName());
    }

    @Test
    void testResolveUser_FuzzyMatch() {
        User u1 = new User("alice@example.com", "hash", "Alice Smith");
        User u2 = new User("bob@example.com", "hash", "Bob Jones");
        Membership m1 = new Membership(u1, workspace, Role.MEMBER);
        Membership m2 = new Membership(u2, workspace, Role.MEMBER);
        when(membershipRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(m1, m2));

        Optional<User> result = contextResolver.resolveUser(workspaceId, "Alice");
        assertTrue(result.isPresent());
        assertEquals("Alice Smith", result.get().getFullName());

        Optional<User> result2 = contextResolver.resolveUser(workspaceId, "bob@example.com");
        assertTrue(result2.isPresent());
        assertEquals("Bob Jones", result2.get().getFullName());
    }

    @Test
    void testResolveDocument_FuzzyMatch() {
        Document d1 = new Document(workspace, "Q3 Goals", "Content");
        Document d2 = new Document(workspace, "API Specs", "Content");
        when(documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)).thenReturn(List.of(d1, d2));

        Optional<Document> result = contextResolver.resolveDocument(workspaceId, "Goals");
        assertTrue(result.isPresent());
        assertEquals("Q3 Goals", result.get().getTitle());
    }

    @Test
    void testResolveChannel_FuzzyMatch() {
        Channel c1 = new Channel(workspace, "general");
        Channel c2 = new Channel(workspace, "random");
        when(channelRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(c1, c2));

        Optional<Channel> result = contextResolver.resolveChannel(workspaceId, "gen");
        assertTrue(result.isPresent());
        assertEquals("general", result.get().getName());
    }

    @Test
    void testBuildWorkspaceContextPrompt() {
        Project p1 = new Project(workspace, "Q3 Board", "Desc");
        when(projectRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(p1));

        Channel c1 = new Channel(workspace, "general");
        ReflectionTestUtils.setField(c1, "id", UUID.randomUUID());
        when(channelRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(c1));

        Document d1 = new Document(workspace, "Design Doc", "Content");
        when(documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)).thenReturn(List.of(d1));

        User u1 = new User("alice@example.com", "hash", "Alice Smith");
        Membership m1 = new Membership(u1, workspace, Role.MEMBER);
        when(membershipRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(m1));

        String prompt = contextResolver.buildWorkspaceContextPrompt(workspaceId, c1.getId());
        assertTrue(prompt.contains("- Available Projects: Q3 Board"));
        assertTrue(prompt.contains("- Available Channels: general"));
        assertTrue(prompt.contains("- Current Channel: general"));
        assertTrue(prompt.contains("- Available Documents: Design Doc"));
        assertTrue(prompt.contains("- Workspace Members: Alice Smith"));
    }
}
