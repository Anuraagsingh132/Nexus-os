package com.nexusos.api.identity.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import com.nexusos.api.search.repository.DocumentSearchRepository;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JwtServiceTest {

    @MockBean
    private DocumentSearchRepository documentSearchRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void testGenerateAndValidateToken() {
        UserDetails userDetails = new User("test@example.com", "password", Collections.emptyList());

        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = jwtService.extractUsername(token);
        assertEquals("test@example.com", username);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testTokenInvalidForDifferentUser() {
        UserDetails user1 = new User("user1@example.com", "password", Collections.emptyList());
        UserDetails user2 = new User("user2@example.com", "password", Collections.emptyList());

        String token = jwtService.generateToken(user1);

        assertFalse(jwtService.isTokenValid(token, user2));
    }

    @Test
    void testGenerateRefreshToken() {
        UserDetails userDetails = new User("refresh@example.com", "password", Collections.emptyList());

        String refreshToken = jwtService.generateRefreshToken(userDetails);
        assertNotNull(refreshToken);

        String username = jwtService.extractUsername(refreshToken);
        assertEquals("refresh@example.com", username);
    }
}
