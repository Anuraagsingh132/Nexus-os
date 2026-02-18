package com.nexusos.api.chat.controller;

import com.nexusos.api.chat.domain.Channel;
import com.nexusos.api.chat.domain.ChatMessage;
import com.nexusos.api.chat.dto.ChatMessageDto;
import com.nexusos.api.chat.service.ChatService;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.identity.security.CustomUserDetails;
import com.nexusos.api.ai.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nexusos.api.ai.agent.service.AgentService;
import com.nexusos.api.ai.agent.dto.AgentResponse;
import com.nexusos.api.chat.repository.ChatMessageRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;

import com.nexusos.api.workspace.repository.MembershipRepository;
import com.nexusos.api.workspace.domain.Membership;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/channels/{channelName}/messages")
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiService aiService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final java.util.concurrent.Executor taskExecutor;
    private final com.nexusos.api.notifications.service.NotificationService notificationService;
    private final MembershipRepository membershipRepository;
    private final AgentService agentService;
    private final ChatMessageRepository chatMessageRepository;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate, AiService aiService, UserRepository userRepository, PasswordEncoder passwordEncoder, @org.springframework.beans.factory.annotation.Qualifier("taskExecutor") java.util.concurrent.Executor taskExecutor, com.nexusos.api.notifications.service.NotificationService notificationService, MembershipRepository membershipRepository, AgentService agentService, ChatMessageRepository chatMessageRepository) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.aiService = aiService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.taskExecutor = taskExecutor;
        this.notificationService = notificationService;
        this.membershipRepository = membershipRepository;
        this.agentService = agentService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable UUID workspaceId,
            @PathVariable String channelName) {
        
        Channel channel = chatService.getOrCreateChannel(workspaceId, channelName);
        List<ChatMessage> messages = chatService.getMessages(channel.getId());
        
        List<ChatMessageDto> dtos = messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("@workspaceSecurity.isMember(#workspaceId)")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable UUID workspaceId,
            @PathVariable String channelName,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
            
        ChatMessage message = chatService.sendMessage(workspaceId, channelName, userDetails.getUser().getId(), request.content());
        ChatMessageDto dto = toDto(message);
        
        // Broadcast the user's message
        messagingTemplate.convertAndSend("/topic/workspaces/" + workspaceId + "/channels/" + channelName, dto);
        
        // Check for AI mention
        String contentLower = request.content().toLowerCase().trim();
        if (contentLower.startsWith("@ai ") || contentLower.equals("@ai")) {
            String aiQuery = request.content().substring(contentLower.indexOf("@ai") + 3).trim();
            if (aiQuery.isEmpty()) aiQuery = "Hello";
            
            // Generate AI response asynchronously to not block the request
            final String cleanContent = aiQuery;
            final Channel finalChannel = message.getChannel();
            final UUID finalChannelId = finalChannel.getId();
            final UUID finalAuthorId = userDetails.getUser().getId();
            taskExecutor.execute(() -> {
                try {
                    AgentResponse agentResponse = agentService.processRequest(workspaceId, finalAuthorId, cleanContent, finalChannelId);
                    
                    String responseText = (agentResponse != null) ? agentResponse.getTextResponse() : null;
                    
                    if (responseText == null || responseText.isEmpty()) {
                        // Fallback to existing RAG Q&A
                        AiService.AiResult aiResult = aiService.getAiResponse(workspaceId, cleanContent);
                        
                        // Create citations appendix
                        StringBuilder sb = new StringBuilder();
                        sb.append(aiResult.answer());
                        if (!aiResult.citations().isEmpty()) {
                            sb.append("\n\n*Sources:*");
                            for (Map<String, String> cit : aiResult.citations()) {
                                sb.append("\n- ").append(cit.get("title"));
                            }
                        }
                        responseText = sb.toString();
                    }
                    
                    // Get or create AI user
                    User aiUser = userRepository.findByEmail("ai@nexusos.dev")
                            .orElseGet(() -> userRepository.save(new User("ai@nexusos.dev", passwordEncoder.encode("AI_SECRET_123!"), "Nexus AI Assistant")));
                    
                    ChatMessage aiMessage = new ChatMessage(finalChannel, aiUser, responseText);
                    chatMessageRepository.save(aiMessage);
                    ChatMessageDto aiDto = toDto(aiMessage);
                    
                    // Broadcast AI's message
                    messagingTemplate.convertAndSend("/topic/workspaces/" + workspaceId + "/channels/" + channelName, aiDto);
                } catch (Exception e) {
                    log.error("Agent execution failed: {}", e.getMessage(), e);
                    try {
                        User aiUser = userRepository.findByEmail("ai@nexusos.dev").orElseThrow();
                        ChatMessage errorMsg = chatService.sendMessage(workspaceId, channelName, aiUser.getId(), 
                            "⚠️ I'm sorry, but I couldn't process that request: " + e.getMessage());
                        messagingTemplate.convertAndSend("/topic/workspaces/" + workspaceId + "/channels/" + channelName, toDto(errorMsg));
                    } catch (Exception innerE) {
                        log.error("Failed to send AI error message: {}", innerE.getMessage(), innerE);
                    }
                }
            });
        }
        
        // Check for workspace member mentions
        List<Membership> members = membershipRepository.findByWorkspaceId(workspaceId);
        for (Membership m : members) {
            User u = m.getUser();
            if (u.getId().equals(userDetails.getUser().getId())) continue;
            
            String firstName = u.getFullName().contains(" ") ? u.getFullName().split(" ")[0] : u.getFullName();
            if (request.content().contains("@" + u.getFullName()) || request.content().contains("@" + firstName)) {
                notificationService.createAndSendNotification(
                        u.getId(),
                        "New Mention",
                        userDetails.getUser().getFullName() + " mentioned you in #" + channelName
                );
            }
        }

        return ResponseEntity.ok(dto);
    }

    private ChatMessageDto toDto(ChatMessage msg) {
        return new ChatMessageDto(
                msg.getId(),
                msg.getContent(),
                new ChatMessageDto.AuthorDto(msg.getAuthor().getId(), msg.getAuthor().getEmail(), msg.getAuthor().getFullName()),
                msg.getCreatedAt()
        );
    }
}

record SendMessageRequest(String content) {}
