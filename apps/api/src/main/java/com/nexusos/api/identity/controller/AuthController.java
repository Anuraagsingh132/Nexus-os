package com.nexusos.api.identity.controller;

import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.service.AuthService;
import com.nexusos.api.identity.security.TokenBlacklistService;
import com.nexusos.api.identity.security.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final com.nexusos.api.identity.security.JwtService jwtService;

    public AuthController(AuthService authService, TokenBlacklistService tokenBlacklistService,
                          RefreshTokenService refreshTokenService, com.nexusos.api.identity.security.JwtService jwtService) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@jakarta.validation.Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        try {
            User user = authService.signup(request.email(), request.password(), request.fullName());
            AuthService.AuthTokens tokens = authService.authenticate(request.email(), request.password());
            setAuthCookies(response, tokens);
            return ResponseEntity.ok(Map.of("id", user.getId(), "email", user.getEmail(), "message", "User created successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@jakarta.validation.Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.AuthTokens tokens = authService.authenticate(request.email(), request.password());
        setAuthCookies(response, tokens);
        return ResponseEntity.ok(Map.of("message", "Login successful"));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "nexusos_refresh_token", required = false) String refreshToken,
                                     HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token provided"));
        }
        try {
            AuthService.AuthTokens newTokens = authService.refreshToken(refreshToken);
            setAuthCookies(response, newTokens);
            return ResponseEntity.ok(Map.of("message", "Token refreshed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
    
    private void setAuthCookies(HttpServletResponse response, AuthService.AuthTokens tokens) {
        org.springframework.http.ResponseCookie accessCookie = org.springframework.http.ResponseCookie.from("nexusos_access_token", tokens.accessToken())
                .httpOnly(true)
                .path("/")
                .maxAge(900)
                .sameSite("Lax")
                .build();
        
        org.springframework.http.ResponseCookie refreshCookie = org.springframework.http.ResponseCookie.from("nexusos_refresh_token", tokens.refreshToken())
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(604800)
                .sameSite("Lax")
                .build();
        
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "nexusos_access_token", required = false) String accessToken,
            @CookieValue(name = "nexusos_refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        
        if (accessToken != null) {
            long remaining = jwtService.getRemainingTimeInSeconds(accessToken);
            tokenBlacklistService.blacklistToken(accessToken, remaining);
        }
        
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        
        org.springframework.http.ResponseCookie accessCookie = org.springframework.http.ResponseCookie.from("nexusos_access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        
        org.springframework.http.ResponseCookie refreshCookie = org.springframework.http.ResponseCookie.from("nexusos_refresh_token", "")
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(Map.of("email", authentication.getName()));
    }
}

record SignupRequest(
        @jakarta.validation.constraints.NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Invalid email format")
        String email,

        @jakarta.validation.constraints.NotBlank(message = "Password is required")
        @jakarta.validation.constraints.Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @jakarta.validation.constraints.NotBlank(message = "Full name is required")
        String fullName
) {}
record LoginRequest(String email, String password) {}
