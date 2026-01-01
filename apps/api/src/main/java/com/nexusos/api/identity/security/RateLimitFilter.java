package com.nexusos.api.identity.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int loginIpLimit;
    private final int loginEmailLimit;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${nexusos.rate-limit.login-ip-per-minute}") int loginIpLimit,
            @Value("${nexusos.rate-limit.login-email-per-minute}") int loginEmailLimit) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.loginIpLimit = loginIpLimit;
        this.loginEmailLimit = loginEmailLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && "/api/v1/auth/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyRequest wrappedRequest = new CachedBodyRequest(request);
        String ip = clientIp(request);
        String email = loginEmail(wrappedRequest.getCachedBody());

        if (isLimited("rate:login:ip:" + ip, loginIpLimit) || (email != null && isLimited("rate:login:email:" + email, loginEmailLimit))) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many login attempts\"}");
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isLimited(String key, int limit) {
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return attempts != null && attempts > limit;
    }

    private String loginEmail(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode email = root.get("email");
            if (email == null || email.asText().isBlank()) {
                return null;
            }
            return email.asText().trim().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the client IP address. We intentionally do NOT trust X-Forwarded-For
     * because it is trivially spoofable by any client. In a production setup behind
     * a trusted reverse proxy, Spring's ForwardedHeaderFilter should be used instead,
     * which rewrites getRemoteAddr() based on a trusted proxy configuration.
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }
    }
}
