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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_READ_BYTES = 8192; // 8 KB safety cap to prevent OOM attacks

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
        String ip = resolveClientIp(request);

        // Check if content-length explicitly exceeds maximum allowed login payload size
        long contentLength = request.getContentLengthLong();
        byte[] boundedBody = null;

        if (contentLength <= MAX_BODY_READ_BYTES) {
            boundedBody = readBoundedStream(request.getInputStream(), MAX_BODY_READ_BYTES);
        }

        String email = (boundedBody != null && boundedBody.length > 0) ? extractLoginEmail(boundedBody) : null;

        if (isLimited("rate:login:ip:" + ip, loginIpLimit) || (email != null && isLimited("rate:login:email:" + email, loginEmailLimit))) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many login attempts. Please try again later.\"}");
            return;
        }

        if (boundedBody != null) {
            HttpServletRequest wrappedRequest = new BoundedBodyRequest(request, boundedBody);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isLimited(String key, int limit) {
        try {
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            return attempts != null && attempts > limit;
        } catch (Exception e) {
            // Fail open on Redis connectivity failure for rate-limiting
            return false;
        }
    }

    private String extractLoginEmail(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode emailNode = root.get("email");
            if (emailNode == null || emailNode.asText().isBlank()) {
                return null;
            }
            return emailNode.asText().trim().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely resolves the real client IP address. Parses X-Forwarded-For if present,
     * extracting the client IP (rightmost untrusted IP in proxy chain), stripping whitespace,
     * falling back to request.getRemoteAddr().
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] ips = xff.split(",");
            for (int i = ips.length - 1; i >= 0; i--) {
                String candidate = ips[i].trim();
                if (!candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private byte[] readBoundedStream(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        int totalRead = 0;
        while ((bytesRead = input.read(data, 0, Math.min(data.length, maxBytes - totalRead))) != -1) {
            buffer.write(data, 0, bytesRead);
            totalRead += bytesRead;
            if (totalRead >= maxBytes) {
                break;
            }
        }
        return buffer.toByteArray();
    }

    private static class BoundedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        BoundedBodyRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
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
