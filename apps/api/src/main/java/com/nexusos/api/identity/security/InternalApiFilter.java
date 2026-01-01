package com.nexusos.api.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiFilter extends OncePerRequestFilter {

    private final byte[] internalApiSecret;

    public InternalApiFilter(@Value("${nexusos.internal-api-secret}") String internalApiSecret) {
        this.internalApiSecret = internalApiSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedSecret = request.getHeader("X-Internal-Secret");
        if (providedSecret == null || !MessageDigest.isEqual(internalApiSecret, providedSecret.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal API secret");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
