package com.nexusos.api.websocket;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class WsTicketService {

    private final StringRedisTemplate redisTemplate;
    private static final String TICKET_PREFIX = "ws:ticket:";
    private static final long TICKET_TTL_SECONDS = 30;

    public WsTicketService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateTicket(String userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                TICKET_PREFIX + token,
                userId,
                Duration.ofSeconds(TICKET_TTL_SECONDS)
        );
        return token;
    }

    public String consumeTicket(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return redisTemplate.opsForValue().getAndDelete(TICKET_PREFIX + token);
    }
}
