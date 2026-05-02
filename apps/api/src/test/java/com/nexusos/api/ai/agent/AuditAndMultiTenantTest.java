package com.nexusos.api.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusos.api.ai.agent.service.AgentService;
import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.ai.agent.tool.AgentTool;
import com.nexusos.api.ai.agent.tool.ToolResult;
import com.nexusos.api.ai.entity.AgentActivity;
import com.nexusos.api.ai.repository.AgentActivityRepository;
import com.nexusos.api.ai.provider.TaskRouter;
import com.nexusos.api.ai.service.AiService;
import com.nexusos.api.common.domain.AuditLog;
import com.nexusos.api.common.repository.AuditLogRepository;
import com.nexusos.api.projects.domain.Project;
import com.nexusos.api.projects.repository.ProjectRepository;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditAndMultiTenantTest {

    @Mock
    private ContextResolver contextResolver;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private AiService aiService;
    @Mock
    private AgentActivityRepository agentActivityRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TaskRouter taskRouter;
    @Mock
    private AgentTool agentTool;
    @Mock
    private ProjectRepository projectRepository;

    private AgentService agentService;
    private ContextResolver actualContextResolver;
    private ObjectMapper objectMapper;

    private UUID workspaceId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        objectMapper = new ObjectMapper();

        when(agentTool.getName()).thenReturn("test_tool");
        agentService = new AgentService(contextResolver, workspaceRepository, aiService,
                agentActivityRepository, auditLogRepository, objectMapper, taskRouter, List.of(agentTool));

        actualContextResolver = new ContextResolver(projectRepository, null, null, null);
    }

    @Test
    void testConfirmActivityWritesAuditLog() throws Exception {
        // Test that confirming a pending activity writes an audit log
        when(agentTool.execute(eq(workspaceId), eq(userId), any()))
                .thenReturn(new ToolResult(true, "Success", null, null));

        AgentActivity activity = AgentActivity.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .toolName("test_tool")
                .argumentsJson("{\"key\":\"value\"}")
                .status("PENDING_CONFIRMATION")
                .build();

        when(agentActivityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(agentActivityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        agentService.confirmActivity(workspaceId, activity.getId(), userId);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());

        AuditLog savedLog = auditCaptor.getValue();
        assertEquals("AGENT", savedLog.getSource());
        assertEquals("AGENT_TOOL:test_tool", savedLog.getAction());
        assertNotNull(savedLog.getTimestamp());
        assertEquals(userId, savedLog.getUserId());
    }

    @Test
    void testCrossWorkspaceDataLookupReturnsEmpty() {
        Workspace workspace1 = mock(Workspace.class);
        new Project(workspace1, "Secret Project", "Desc");

        when(projectRepository.findByWorkspaceId(workspaceId)).thenReturn(Collections.emptyList());

        Optional<Project> result = actualContextResolver.resolveProject(workspaceId, "Secret Project");

        assertTrue(result.isEmpty());
    }
}
