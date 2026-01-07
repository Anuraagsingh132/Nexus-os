package com.nexusos.api.websocket;

import com.nexusos.api.identity.security.CustomUserDetails;
import com.nexusos.api.content.domain.Document;
import com.nexusos.api.content.repository.DocumentRepository;
import com.nexusos.api.workspace.repository.MembershipRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@RestController
public class WsTicketController {

    private final WsTicketService ticketService;
    private final DocumentRepository documentRepository;
    private final MembershipRepository membershipRepository;

    public WsTicketController(
            WsTicketService ticketService,
            DocumentRepository documentRepository,
            MembershipRepository membershipRepository) {
        this.ticketService = ticketService;
        this.documentRepository = documentRepository;
        this.membershipRepository = membershipRepository;
    }

    @PostMapping("/api/v1/ws/ticket")
    public ResponseEntity<Map<String, String>> generateTicket(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String ticket = ticketService.generateTicket(userDetails.getUser().getId().toString());
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    @PostMapping("/api/v1/internal/ws/validate-ticket")
    public ResponseEntity<Void> validateTicketForDocument(@RequestBody ValidateWsTicketRequest request) {
        UUID documentId;
        try {
            documentId = UUID.fromString(request.documentName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        String userIdValue = ticketService.consumeTicket(request.token());
        if (userIdValue == null) {
            return ResponseEntity.status(401).build();
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdValue);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }

        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        if (!membershipRepository.existsByWorkspaceIdAndUserId(document.getWorkspace().getId(), userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok().build();
    }

    record ValidateWsTicketRequest(String token, String documentName) {}
}
