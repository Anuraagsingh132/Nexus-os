package com.nexusos.api.chat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Pattern WORKSPACE_TOPIC_PATTERN = Pattern.compile("^/topic/workspaces/([^/]+)(/.*)?$");

    private final com.nexusos.api.websocket.WsTicketService wsTicketService;
    private final com.nexusos.api.workspace.repository.MembershipRepository membershipRepository;

    public WebSocketAuthInterceptor(
            com.nexusos.api.websocket.WsTicketService wsTicketService,
            com.nexusos.api.workspace.repository.MembershipRepository membershipRepository) {
        this.wsTicketService = wsTicketService;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String ticket = accessor.getFirstNativeHeader("ticket");
            
            if (ticket == null || ticket.isEmpty()) {
                ticket = accessor.getLogin();
            }
            
            if (ticket == null || ticket.isEmpty()) {
                if (accessor.getSessionAttributes() != null && accessor.getSessionAttributes().containsKey("ticket")) {
                    ticket = (String) accessor.getSessionAttributes().get("ticket");
                }
            }

            if (ticket != null && !ticket.isEmpty()) {
                String userId = wsTicketService.consumeTicket(ticket);
                if (userId != null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, java.util.Collections.emptyList()
                    );
                    accessor.setUser(authentication);
                    return message;
                }
            }
            throw new IllegalArgumentException("Invalid or expired websocket ticket");
        }
        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeWorkspaceSubscription(accessor);
        }
        return message;
    }

    private void authorizeWorkspaceSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new IllegalArgumentException("Missing websocket subscription destination");
        }

        Matcher matcher = WORKSPACE_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        UUID workspaceId;
        try {
            workspaceId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid websocket workspace destination");
        }

        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Unauthenticated websocket subscription");
        }

        UUID userId;
        try {
            userId = UUID.fromString(accessor.getUser().getName());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid websocket principal");
        }

        if (!membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new IllegalArgumentException("Forbidden websocket subscription");
        }
    }
}
