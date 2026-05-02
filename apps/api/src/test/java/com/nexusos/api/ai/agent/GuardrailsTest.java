package com.nexusos.api.ai.agent;

import com.nexusos.api.ai.agent.dto.AgentResponse;
import com.nexusos.api.ai.agent.service.AgentService;
import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.ai.agent.tool.AgentTool;
import com.nexusos.api.ai.agent.tool.ToolResult;
import com.nexusos.api.ai.entity.AgentActivity;
import com.nexusos.api.ai.repository.AgentActivityRepository;
import com.nexusos.api.ai.provider.AiProviderAdapter;
import com.nexusos.api.ai.provider.TaskRouter;
import com.nexusos.api.ai.service.AiService;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nexusos.api.common.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GuardrailsTest {

    private AgentService agentService;
    private ContextResolver contextResolver;
    private WorkspaceRepository workspaceRepository;
    private AiService aiService;
    private AgentActivityRepository agentActivityRepository;
    private AuditLogRepository auditLogRepository;
    private TaskRouter taskRouter;
    private AiProviderAdapter mockAdapter;
    private AgentTool highImpactTool;
    private AgentTool lowImpactTool;

    private UUID workspaceId = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private UUID activityId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contextResolver = mock(ContextResolver.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        aiService = mock(AiService.class);
        agentActivityRepository = mock(AgentActivityRepository.class);
        taskRouter = mock(TaskRouter.class);
        mockAdapter = mock(AiProviderAdapter.class);

        highImpactTool = mock(AgentTool.class);
        when(highImpactTool.getName()).thenReturn("highImpactTool");
        when(highImpactTool.isHighImpact()).thenReturn(true);
        when(highImpactTool.getDescription()).thenReturn("A high impact tool");
        when(highImpactTool.getParameterSchema()).thenReturn(Map.of("type", "object", "properties", Map.of()));
        when(highImpactTool.execute(any(), any(), any())).thenReturn(new ToolResult(true, "Success", null, null));

        lowImpactTool = mock(AgentTool.class);
        when(lowImpactTool.getName()).thenReturn("lowImpactTool");
        when(lowImpactTool.isHighImpact()).thenReturn(false);
        when(lowImpactTool.getDescription()).thenReturn("A low impact tool");
        when(lowImpactTool.getParameterSchema()).thenReturn(Map.of("type", "object", "properties", Map.of()));
        when(lowImpactTool.execute(any(), any(), any())).thenReturn(new ToolResult(true, "Success", null, null));

        when(agentActivityRepository.save(any())).thenAnswer(invocation -> {
            AgentActivity activity = invocation.getArgument(0);
            if (activity.getId() == null) {
                activity.setId(UUID.randomUUID());
            }
            return activity;
        });

        auditLogRepository = mock(AuditLogRepository.class);

        agentService = new AgentService(contextResolver, workspaceRepository, aiService,
                agentActivityRepository, auditLogRepository, new ObjectMapper(), taskRouter,
                java.util.List.of(highImpactTool, lowImpactTool));
    }

    @Test
    void testHighImpactToolTriggersPendingConfirmation() {
        // Set up workspace in FULL_AGENT mode
        Workspace workspace = mock(Workspace.class);
        when(workspace.getAgentMode()).thenReturn("FULL_AGENT");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(contextResolver.buildWorkspaceContextPrompt(any(), any())).thenReturn("Context");

        // LLM returns a tool call for the high-impact tool
        when(taskRouter.route(any())).thenReturn(mockAdapter);
        when(mockAdapter.generateToolCall(any(), any(), any()))
                .thenReturn("{\"tool\": \"highImpactTool\", \"arguments\": {}}");

        AgentResponse response = agentService.processRequest(workspaceId, userId, "Do something risky", UUID.randomUUID());

        assertTrue(response.isRequiresConfirmation());
        assertNotNull(response.getPendingActivityId());

        ArgumentCaptor<AgentActivity> captor = ArgumentCaptor.forClass(AgentActivity.class);
        verify(agentActivityRepository).save(captor.capture());

        AgentActivity saved = captor.getValue();
        assertEquals("PENDING_CONFIRMATION", saved.getStatus());
        assertTrue(saved.getRequiresConfirmation());
    }

    @Test
    void testConfirmActivityExecutesToolAndUpdateStatusToSuccess() {
        AgentActivity activity = AgentActivity.builder()
                .id(activityId)
                .workspaceId(workspaceId)
                .toolName("highImpactTool")
                .argumentsJson("{}")
                .status("PENDING_CONFIRMATION")
                .build();

        when(agentActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        AgentActivity confirmed = (AgentActivity) agentService.confirmActivity(workspaceId, activityId, userId);

        assertEquals("SUCCESS", confirmed.getStatus());
        verify(highImpactTool).execute(eq(workspaceId), eq(userId), any());
    }

    @Test
    void testCancelActivitySetsStatusToCancelled() {
        AgentActivity activity = AgentActivity.builder()
                .id(activityId)
                .workspaceId(workspaceId)
                .toolName("highImpactTool")
                .status("PENDING_CONFIRMATION")
                .build();

        when(agentActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        AgentActivity cancelled = (AgentActivity) agentService.cancelActivity(workspaceId, activityId, userId);

        assertEquals("CANCELLED", cancelled.getStatus());
        verify(highImpactTool, never()).execute(any(), any(), any());
    }

    @Test
    void testRagOnlyModeDisablesToolExecution() {
        Workspace workspace = mock(Workspace.class);
        when(workspace.getAgentMode()).thenReturn("RAG_ONLY");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        AiService.AiResult aiResult = new AiService.AiResult("Mock Answer", Collections.emptyList());
        when(aiService.getAiResponse(workspaceId, "Hello")).thenReturn(aiResult);

        AgentResponse response = agentService.processRequest(workspaceId, userId, "Hello", UUID.randomUUID());

        assertEquals("Mock Answer", response.getTextResponse());
        assertFalse(response.isRequiresConfirmation());

        verify(contextResolver, never()).buildWorkspaceContextPrompt(any(), any());
    }
}
