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
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
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
        Cookie accessCookie = new Cookie("nexusos_access_token", tokens.accessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(900); // 15 minutes
        // accessCookie.setSecure(true);
        
        Cookie refreshCookie = new Cookie("nexusos_refresh_token", tokens.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setMaxAge(604800); // 7 days
        // refreshCookie.setSecure(true);
        
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
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
        
        Cookie accessCookie = new Cookie("nexusos_access_token", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        
        Cookie refreshCookie = new Cookie("nexusos_refresh_token", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setMaxAge(0);
        
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
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
