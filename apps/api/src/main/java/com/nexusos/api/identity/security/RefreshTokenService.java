package com.nexusos.api.identity.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createRefreshToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        // Store userId associated with this refresh token
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + token, 
                userId.toString(), 
                Duration.ofDays(REFRESH_TOKEN_VALIDITY_DAYS)
        );
        return token;
    }

    public UUID validateAndRevoke(String token) {
        String userIdStr = redisTemplate.opsForValue().getAndDelete(REFRESH_TOKEN_PREFIX + token);
        if (userIdStr != null) {
            return UUID.fromString(userIdStr);
        }
        return null;
    }
    
    public void revoke(String token) {
        if (token != null) {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
        }
    }
}
