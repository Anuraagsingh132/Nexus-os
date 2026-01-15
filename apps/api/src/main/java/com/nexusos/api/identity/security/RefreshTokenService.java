package com.nexusos.api.identity.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, String> fallbackStore = new ConcurrentHashMap<>();
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createRefreshToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + token, 
                    userId.toString(), 
                    Duration.ofDays(REFRESH_TOKEN_VALIDITY_DAYS)
            );
        } catch (Exception e) {
            fallbackStore.put(REFRESH_TOKEN_PREFIX + token, userId.toString());
        }
        return token;
    }

    public UUID validateAndRevoke(String token) {
        String userIdStr = null;
        try {
            userIdStr = redisTemplate.opsForValue().getAndDelete(REFRESH_TOKEN_PREFIX + token);
        } catch (Exception e) {
            userIdStr = fallbackStore.remove(REFRESH_TOKEN_PREFIX + token);
        }
        if (userIdStr != null) {
            return UUID.fromString(userIdStr);
        }
        return null;
    }
    
    public void revoke(String token) {
        if (token != null) {
            try {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
            } catch (Exception e) {
                fallbackStore.remove(REFRESH_TOKEN_PREFIX + token);
            }
        }
    }
}
