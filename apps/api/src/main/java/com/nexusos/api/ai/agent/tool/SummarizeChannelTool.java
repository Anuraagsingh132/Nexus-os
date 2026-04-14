package com.nexusos.api.ai.agent.tool;

import com.nexusos.api.ai.agent.service.ContextResolver;
import com.nexusos.api.ai.provider.AiProviderAdapter;
import com.nexusos.api.ai.provider.AiTaskType;
import com.nexusos.api.ai.provider.TaskRouter;
import com.nexusos.api.chat.domain.Channel;
import com.nexusos.api.chat.domain.ChatMessage;
import com.nexusos.api.chat.service.ChatService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SummarizeChannelTool implements AgentTool {

    private final ChatService chatService;
    private final ContextResolver contextResolver;
    private final TaskRouter taskRouter;

    public SummarizeChannelTool(ChatService chatService, ContextResolver contextResolver, TaskRouter taskRouter) {
        this.chatService = chatService;
        this.contextResolver = contextResolver;
        this.taskRouter = taskRouter;
    }

    @Override
    public String getName() {
        return "summarize_channel";
    }

    @Override
    public String getDescription() {
        return "Summarizes recent messages in a chat channel using AI.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "channel_name_or_id", Map.of("type", "string", "description", "Name or UUID of the channel to summarize"),
                "max_messages", Map.of("type", "integer", "description", "Maximum number of recent messages to include (default 50)")
            ),
            "required", java.util.List.of("channel_name_or_id")
        );
    }

    @Override
    public boolean isHighImpact() {
        return false;
    }

    @Override
    public ToolResult execute(UUID workspaceId, UUID requestingUserId, Map<String, Object> arguments) {
        String channelRef = (String) arguments.get("channel_name_or_id");
        int maxMessages = arguments.containsKey("max_messages") ? ((Number) arguments.get("max_messages")).intValue() : 50;

        UUID channelId = null;
        try {
            channelId = UUID.fromString(channelRef);
        } catch (IllegalArgumentException e) {
            Optional<Channel> channelOpt = contextResolver.resolveChannel(workspaceId, channelRef);
            if (channelOpt.isPresent()) {
                channelId = channelOpt.get().getId();
            }
        }

        if (channelId == null) {
            return ToolResult.builder().success(false)
                    .errorMessage("Channel not found: " + channelRef)
                    .summary("Failed to summarize — channel not found.").build();
        }

        try {
            List<ChatMessage> messages = chatService.getMessages(channelId);
            if (messages.isEmpty()) {
                return ToolResult.builder().success(true)
                        .summary("Channel has no messages to summarize.")
                        .data("No messages found.").build();
            }

            // Take the last N messages
            List<ChatMessage> recentMessages = messages.size() > maxMessages
                    ? messages.subList(messages.size() - maxMessages, messages.size())
                    : messages;

            String transcript = recentMessages.stream()
                    .map(m -> m.getAuthor().getFullName() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));

            // Use AI to summarize
            AiProviderAdapter adapter = taskRouter.route(AiTaskType.SUMMARIZATION);
            String summary = adapter.generateText(
                    "Summarize the following chat conversation concisely, highlighting key decisions, action items, and important topics:\n\n" + transcript,
                    "You are a concise summarizer for a team workspace. Produce a clear bullet-point summary."
            );

            return ToolResult.builder()
                    .success(true)
                    .summary("Channel summarized (" + recentMessages.size() + " messages).")
                    .data(summary)
                    .build();
        } catch (Exception e) {
            return ToolResult.builder().success(false)
                    .errorMessage(e.getMessage())
                    .summary("Failed to summarize channel: " + e.getMessage()).build();
        }
    }
}
