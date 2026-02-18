package com.nexusos.api.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

@Service
public class WsTicketService {

    private static final Logger log = LoggerFactory.getLogger(WsTicketService.class);
    private final StringRedisTemplate redisTemplate;
    private static final String TICKET_PREFIX = "ws:ticket:";
    private static final long TICKET_TTL_SECONDS = 30;

    public WsTicketService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateTicket(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required to generate WebSocket ticket");
        }
        String token = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(
                    TICKET_PREFIX + token,
                    userId,
                    Duration.ofSeconds(TICKET_TTL_SECONDS)
            );
            return token;
        } catch (Exception e) {
            log.error("Redis ticket generation failed: {}", e.getMessage(), e);
            // Fail fast: return HTTP 503 instead of falling back to a per-instance in-memory map
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WebSocket ticket service unavailable (Redis connection error)");
        }
    }

    public String consumeTicket(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().getAndDelete(TICKET_PREFIX + token);
        } catch (Exception e) {
            log.error("Redis ticket consumption failed: {}", e.getMessage(), e);
            // Fail fast: return HTTP 503 instead of per-instance in-memory map
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "WebSocket ticket service unavailable (Redis connection error)");
        }
    }
}
