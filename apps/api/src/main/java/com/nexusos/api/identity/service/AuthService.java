package com.nexusos.api.identity.service;

import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.identity.security.JwtService;
import com.nexusos.api.identity.security.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final com.nexusos.api.notifications.service.NotificationService notificationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService, RefreshTokenService refreshTokenService,
                       com.nexusos.api.notifications.service.NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.notificationService = notificationService;
    }

    @Transactional
    public User signup(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }
        User user = new User(email, passwordEncoder.encode(password), fullName);
        user.setEmailVerified(true);
        user = userRepository.save(user);
        
        notificationService.createAndSendNotification(
            user.getId(),
            "Welcome to Nexus OS!",
            "We are thrilled to have you here, " + fullName + "."
        );
        
        return user;
    }

    public record AuthTokens(String accessToken, String refreshToken) {}

    @Transactional
    public AuthTokens authenticate(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        User user = userRepository.findByEmail(email).orElseThrow();
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return new AuthTokens(accessToken, refreshToken);
    }
    
    @Transactional
    public AuthTokens refreshToken(String refreshToken) {
        java.util.UUID userId = refreshTokenService.validateAndRevoke(refreshToken);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = refreshTokenService.createRefreshToken(userId);
        return new AuthTokens(newAccessToken, newRefreshToken);
    }
}
