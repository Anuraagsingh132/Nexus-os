package com.nexusos.api.ai.agent.service;

import com.nexusos.api.ai.agent.dto.AgentResponse;
import com.nexusos.api.ai.agent.dto.AgentSettingsDto;
import com.nexusos.api.ai.agent.tool.AgentTool;
import com.nexusos.api.ai.agent.tool.ToolResult;
import com.nexusos.api.ai.entity.AgentActivity;
import com.nexusos.api.ai.repository.AgentActivityRepository;
import com.nexusos.api.ai.provider.AiProviderAdapter;
import com.nexusos.api.ai.provider.AiTaskType;
import com.nexusos.api.ai.provider.TaskRouter;
import com.nexusos.api.ai.service.AiService;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import com.nexusos.api.common.domain.AuditLog;
import com.nexusos.api.common.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ContextResolver contextResolver;
    private final WorkspaceRepository workspaceRepository;
    private final AiService aiService;
    private final AgentActivityRepository agentActivityRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final TaskRouter taskRouter;
    private final Map<String, AgentTool> tools;

    public AgentService(ContextResolver contextResolver, WorkspaceRepository workspaceRepository,
                        AiService aiService, AgentActivityRepository agentActivityRepository,
                        AuditLogRepository auditLogRepository, ObjectMapper objectMapper,
                        TaskRouter taskRouter, List<AgentTool> agentTools) {
        this.contextResolver = contextResolver;
        this.workspaceRepository = workspaceRepository;
        this.aiService = aiService;
        this.agentActivityRepository = agentActivityRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.taskRouter = taskRouter;
        this.tools = agentTools != null
                ? agentTools.stream().collect(Collectors.toMap(AgentTool::getName, Function.identity()))
                : Collections.emptyMap();
    }

    private static final int MAX_STEPS = 5;

    public AgentSettingsDto getAgentSettings(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        return new AgentSettingsDto(workspace.getAgentEnabled(), workspace.getAgentMode(), "openai", Collections.emptyMap());
    }

    public AgentSettingsDto updateAgentSettings(UUID workspaceId, AgentSettingsDto settings) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        if (settings.getAgentEnabled() != null) workspace.setAgentEnabled(settings.getAgentEnabled());
        if (settings.getAgentMode() != null) workspace.setAgentMode(settings.getAgentMode());
        workspaceRepository.save(workspace);
        return new AgentSettingsDto(workspace.getAgentEnabled(), workspace.getAgentMode(), "openai", Collections.emptyMap());
    }

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public java.util.concurrent.CompletableFuture<AgentResponse> processRequestAsync(UUID workspaceId, UUID userId, String userMessage, UUID sourceChannelId) {
        return java.util.concurrent.CompletableFuture.completedFuture(processRequest(workspaceId, userId, userMessage, sourceChannelId));
    }

    public AgentResponse processRequest(UUID workspaceId, UUID userId, String userMessage, UUID sourceChannelId) {
        log.info("Processing agent request for workspace={}, user={}", workspaceId, userId);

        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();

        // RAG_ONLY mode: bypass agent orchestration, go straight to knowledge search
        if ("RAG_ONLY".equals(workspace.getAgentMode())) {
            var aiResult = aiService.getAiResponse(workspaceId, userMessage);
            return AgentResponse.builder()
                .textResponse(aiResult.answer())
                .requiresConfirmation(false)
                .build();
        }

        // Build workspace context for the LLM
        String workspaceContext = contextResolver.buildWorkspaceContextPrompt(workspaceId, sourceChannelId);

        // Build tools schema for the LLM
        String toolsSchema = buildToolsJsonSchema();

        // Build system prompt
        String systemPrompt = buildSystemPrompt(workspaceContext);

        // Orchestration loop: multi-step tool calling with a cap
        StringBuilder conversationHistory = new StringBuilder();
        conversationHistory.append("User request: ").append(userMessage).append("\n");

        for (int step = 1; step <= MAX_STEPS; step++) {
            log.debug("Agent orchestration step {} of {}", step, MAX_STEPS);

            AiProviderAdapter adapter = taskRouter.route(AiTaskType.AGENT_TOOL_CALLING);
            String llmResponse;
            try {
                llmResponse = adapter.generateToolCall(conversationHistory.toString(), systemPrompt, toolsSchema);
            } catch (Exception e) {
                log.error("LLM call failed at step {}", step, e);
                return AgentResponse.builder()
                        .textResponse("I'm sorry, I encountered an error communicating with the AI provider. Please try again.")
                        .requiresConfirmation(false)
                        .build();
            }

            log.debug("LLM response at step {}: {}", step, llmResponse);

            // Parse the JSON tool call from the LLM response
            JsonNode parsed = parseToolCallJson(llmResponse);
            if (parsed == null) {
                // Retry once: ask the LLM to fix its JSON
                log.warn("Invalid JSON from LLM, retrying once");
                conversationHistory.append("System: Your previous response was not valid JSON. Please respond with ONLY valid JSON.\n");
                try {
                    llmResponse = adapter.generateToolCall(conversationHistory.toString(), systemPrompt, toolsSchema);
                    parsed = parseToolCallJson(llmResponse);
                } catch (Exception e) {
                    log.error("LLM retry failed", e);
                }
                if (parsed == null) {
                    // If still not valid JSON, treat the raw text as a direct answer
                    return AgentResponse.builder()
                            .textResponse(llmResponse)
                            .requiresConfirmation(false)
                            .build();
                }
            }

            String toolName = parsed.path("tool").asText("__none__");

            // If the LLM chose not to use a tool, return its text response
            if ("__none__".equals(toolName) || toolName.isEmpty()) {
                String directResponse = parsed.path("response").asText(
                        parsed.has("response") ? "" : "Request processed."
                );
                return AgentResponse.builder()
                        .textResponse(directResponse)
                        .requiresConfirmation(false)
                        .build();
            }

            // Look up the tool
            AgentTool tool = tools.get(toolName);
            if (tool == null) {
                conversationHistory.append("System: Tool '").append(toolName)
                        .append("' does not exist. Available tools: ")
                        .append(String.join(", ", tools.keySet())).append("\n");
                continue;
            }

            // Parse arguments
            Map<String, Object> arguments;
            try {
                arguments = objectMapper.convertValue(parsed.path("arguments"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                arguments = Collections.emptyMap();
            }

            // Guardrail: high-impact tools require confirmation
            if (tool.isHighImpact()) {
                String argsJson;
                try {
                    argsJson = objectMapper.writeValueAsString(arguments);
                } catch (JsonProcessingException e) {
                    argsJson = arguments.toString();
                }

                AgentActivity activity = AgentActivity.builder()
                        .workspaceId(workspaceId)
                        .requestedBy(userId)
                        .toolName(toolName)
                        .argumentsJson(argsJson)
                        .status("PENDING_CONFIRMATION")
                        .requiresConfirmation(true)
                        .sourceChannelId(sourceChannelId)
                        .build();
                activity = agentActivityRepository.save(activity);

                return AgentResponse.builder()
                        .requiresConfirmation(true)
                        .pendingActivityId(activity.getId())
                        .textResponse("I'd like to execute **" + tool.getDescription() + "** with the provided parameters. This action requires your confirmation.")
                        .build();
            }

            // Execute the tool
            ToolResult result;
            try {
                result = tool.execute(workspaceId, userId, arguments);
            } catch (Exception e) {
                log.error("Tool execution failed: {}", toolName, e);
                result = ToolResult.builder()
                        .success(false)
                        .errorMessage(e.getMessage())
                        .summary("Tool execution failed: " + e.getMessage())
                        .build();
            }

            // Record activity
            String argsJson;
            try {
                argsJson = objectMapper.writeValueAsString(arguments);
            } catch (JsonProcessingException e) {
                argsJson = arguments.toString();
            }

            AgentActivity activity = AgentActivity.builder()
                    .workspaceId(workspaceId)
                    .requestedBy(userId)
                    .toolName(toolName)
                    .argumentsJson(argsJson)
                    .status(result.isSuccess() ? "SUCCESS" : "FAILED")
                    .resultSummary(result.getSummary())
                    .errorMessage(result.getErrorMessage())
                    .requiresConfirmation(false)
                    .sourceChannelId(sourceChannelId)
                    .build();
            agentActivityRepository.save(activity);

            // Audit log
            recordAuditLog(toolName, userId, arguments, result);

            // If the tool failed, report back
            if (!result.isSuccess()) {
                return AgentResponse.builder()
                        .textResponse("I tried to use **" + toolName + "** but it failed: " + result.getSummary())
                        .requiresConfirmation(false)
                        .build();
            }

            // Append tool result to conversation so LLM can decide next step
            conversationHistory.append("Tool result (").append(toolName).append("): ")
                    .append(result.getSummary()).append("\n");

            // If this is the last step, break and summarize
            if (step == MAX_STEPS) {
                return AgentResponse.builder()
                        .textResponse("Done! " + result.getSummary())
                        .requiresConfirmation(false)
                        .build();
            }
        }

        return AgentResponse.builder()
                .textResponse("I've completed processing your request.")
                .requiresConfirmation(false)
                .build();
    }

    @Transactional
    public Object confirmActivity(UUID workspaceId, UUID activityId, UUID userId) {
        log.info("Confirming activity={} by user={} in workspace={}", activityId, userId, workspaceId);
        AgentActivity activity = agentActivityRepository.findById(activityId).orElseThrow();

        if (!activity.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Activity does not belong to this workspace");
        }
        if (!"PENDING_CONFIRMATION".equals(activity.getStatus())) {
            throw new IllegalStateException("Activity is not pending confirmation");
        }

        AgentTool tool = tools.get(activity.getToolName());
        if (tool == null) {
            activity.setStatus("FAILED");
            activity.setErrorMessage("Tool no longer available: " + activity.getToolName());
            return agentActivityRepository.save(activity);
        }

        // Parse saved arguments
        Map<String, Object> arguments;
        try {
            arguments = objectMapper.readValue(
                    activity.getArgumentsJson() != null ? activity.getArgumentsJson() : "{}",
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            arguments = Collections.emptyMap();
        }

        ToolResult result = tool.execute(workspaceId, userId, arguments);

        activity.setStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        activity.setResultSummary(result.getSummary());
        activity.setErrorMessage(result.getErrorMessage());
        activity.setConfirmedBy(userId);
        agentActivityRepository.save(activity);

        recordAuditLog(activity.getToolName(), userId, arguments, result);

        return activity;
    }

    @Transactional
    public Object cancelActivity(UUID workspaceId, UUID activityId, UUID userId) {
        log.info("Canceling activity={} by user={} in workspace={}", activityId, userId, workspaceId);
        AgentActivity activity = agentActivityRepository.findById(activityId).orElseThrow();

        if (!activity.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Activity does not belong to this workspace");
        }
        if (!"PENDING_CONFIRMATION".equals(activity.getStatus())) {
            throw new IllegalStateException("Activity is not pending confirmation");
        }
        activity.setStatus("CANCELLED");
        return agentActivityRepository.save(activity);
    }

    @Scheduled(fixedDelay = 60000)
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name = "AgentService_expirePendingConfirmations", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    @Transactional
    public void expirePendingConfirmations() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(10);
        List<AgentActivity> pendingActivities = agentActivityRepository.findByStatusAndCreatedAtBefore("PENDING_CONFIRMATION", threshold);
        if (!pendingActivities.isEmpty()) {
            log.info("Expiring {} pending agent confirmations", pendingActivities.size());
            for (AgentActivity activity : pendingActivities) {
                activity.setStatus("EXPIRED");
            }
            agentActivityRepository.saveAll(pendingActivities);
        }
    }

    // --- Private helpers ---

    private String buildSystemPrompt(String workspaceContext) {
        return """
                You are Nexus AI, an intelligent agent assistant embedded in the Nexus OS workspace.
                You help users manage their workspace by creating tasks, updating statuses, creating documents, \
                searching knowledge, summarizing channels, and more.

                RULES:
                1. When the user asks you to perform an action, choose the most appropriate tool.
                2. You MUST respond with ONLY a valid JSON object.
                3. If you want to call a tool: {"tool": "tool_name", "arguments": {...}}
                4. If you want to answer a question directly without a tool: {"tool": "__none__", "arguments": {}, "response": "your text answer"}
                5. Use the workspace context below to resolve names to actual entities.
                6. Be concise and helpful.

                """ + workspaceContext;
    }

    private String buildToolsJsonSchema() {
        List<Map<String, Object>> toolDescriptions = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            toolDescriptions.add(Map.of(
                    "name", tool.getName(),
                    "description", tool.getDescription(),
                    "parameters", tool.getParameterSchema(),
                    "high_impact", tool.isHighImpact()
            ));
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolDescriptions);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private JsonNode parseToolCallJson(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return null;
        }

        // Try to extract JSON from the response (LLMs sometimes wrap in ```json blocks)
        String cleaned = llmResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        // Find the first { and last } to extract JSON object
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            return null;
        }
        cleaned = cleaned.substring(start, end + 1);

        try {
            JsonNode node = objectMapper.readTree(cleaned);
            if (node.isObject() && node.has("tool")) {
                return node;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void recordAuditLog(String toolName, UUID userId, Map<String, Object> arguments, ToolResult result) {
        try {
            String metadata = objectMapper.writeValueAsString(Map.of(
                    "arguments", arguments,
                    "success", result.isSuccess(),
                    "summary", result.getSummary() != null ? result.getSummary() : ""
            ));
            auditLogRepository.save(new AuditLog("AGENT_TOOL:" + toolName, userId, Instant.now(), "AGENT", metadata));
        } catch (Exception e) {
            log.error("Failed to serialize audit log metadata", e);
        }
    }
}
