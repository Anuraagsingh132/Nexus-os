package com.nexusos.api.ai.agent;

import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.ai.agent.tool.*;
import com.nexusos.api.ai.service.AiService;
import com.nexusos.api.calendar.service.MeetingService;
import com.nexusos.api.content.service.DocumentService;
import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.projects.repository.TaskRepository;
import com.nexusos.api.projects.service.ProjectService;
import com.nexusos.api.workspace.service.WorkspaceInviteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.content.domain.Document;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase5ToolsTest {

    @Mock private ProjectService projectService;
    @Mock private ContextResolver contextResolver;
    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private MeetingService meetingService;
    @Mock private WorkspaceInviteService workspaceInviteService;
    @Mock private AiService aiService;
    @Mock private DocumentService documentService;

    private ReassignTaskTool reassignTaskTool;
    private BulkUpdateTaskStatusTool bulkUpdateTaskStatusTool;
    private DraftCalendarMeetingTool draftCalendarMeetingTool;
    private DraftWorkspaceInviteTool draftWorkspaceInviteTool;
    private GenerateDocumentFromSpecTool generateDocumentFromSpecTool;

    @BeforeEach
    void setUp() {
        reassignTaskTool = new ReassignTaskTool(projectService, contextResolver, taskRepository);
        bulkUpdateTaskStatusTool = new BulkUpdateTaskStatusTool(projectService, contextResolver);
        draftCalendarMeetingTool = new DraftCalendarMeetingTool(meetingService);
        draftWorkspaceInviteTool = new DraftWorkspaceInviteTool(workspaceInviteService);
        generateDocumentFromSpecTool = new GenerateDocumentFromSpecTool(aiService, documentService);
    }

    @Test
    void testRegistrationAndSchema() {
        assertEquals("reassign_task", reassignTaskTool.getName());
        assertTrue(reassignTaskTool.isHighImpact());
        assertTrue(reassignTaskTool.getParameterSchema().containsKey("properties"));

        assertEquals("bulk_update_task_status", bulkUpdateTaskStatusTool.getName());
        assertTrue(bulkUpdateTaskStatusTool.isHighImpact());

        assertEquals("draft_calendar_meeting", draftCalendarMeetingTool.getName());
        assertFalse(draftCalendarMeetingTool.isHighImpact());

        assertEquals("draft_workspace_invite", draftWorkspaceInviteTool.getName());
        assertTrue(draftWorkspaceInviteTool.isHighImpact());

        assertEquals("generate_document_from_spec", generateDocumentFromSpecTool.getName());
        assertFalse(generateDocumentFromSpecTool.isHighImpact());
    }

    @Test
    void testDraftWorkspaceInviteTool() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        ToolResult result = draftWorkspaceInviteTool.execute(workspaceId, userId, Map.of("email", "test@example.com"));
        
        assertTrue(result.isSuccess());
        verify(workspaceInviteService).inviteUserToWorkspace(workspaceId, "test@example.com");
    }

    @Test
    void testDraftCalendarMeetingTool() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        
        ToolResult result = draftCalendarMeetingTool.execute(workspaceId, userId, Map.of(
            "title", "Sync",
            "start_time", start.toString(),
            "end_time", end.toString(),
            "video_url", "http://meet.com/abc"
        ));
        
        assertTrue(result.isSuccess());
        verify(meetingService).createMeeting(workspaceId, "Sync", start, end, "http://meet.com/abc");
    }

    @Test
    void testGenerateDocumentFromSpecTool() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        when(aiService.getAiResponse(eq(workspaceId), anyString()))
            .thenReturn(new AiService.AiResult("Generated Content", java.util.List.of()));
            
        Document doc = mock(Document.class);
        when(doc.getId()).thenReturn(UUID.randomUUID());
        when(documentService.createDocument(workspaceId, "Generated Document", "Generated Content"))
            .thenReturn(doc);
            
        ToolResult result = generateDocumentFromSpecTool.execute(workspaceId, userId, Map.of(
            "spec_query", "query",
            "instruction", "inst"
        ));
        
        assertTrue(result.isSuccess());
        verify(documentService).createDocument(workspaceId, "Generated Document", "Generated Content");
    }

    @Test
    void testReassignTaskTool() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getFullName()).thenReturn("John Doe");
        
        when(contextResolver.resolveUser(workspaceId, "John")).thenReturn(Optional.of(user));
        
        ToolResult result = reassignTaskTool.execute(workspaceId, userId, Map.of(
            "task_title_or_id", taskId.toString(),
            "new_assignee_name", "John"
        ));
        
        assertTrue(result.isSuccess());
        verify(projectService).reassignTask(workspaceId, taskId, userId);
    }
}
