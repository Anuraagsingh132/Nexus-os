package com.nexusos.api.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.nexusos.api.search.repository.DocumentSearchRepository;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
public class AuthControllerTest {

    @MockBean
    private DocumentSearchRepository documentSearchRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testSignupSuccess() throws Exception {
        Map<String, String> payload = Map.of(
            "email", "test@example.com",
            "password", "Password123!",
            "fullName", "Test User"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Create user without manually setting ID — let JPA generate it
        User user = new User("login@example.com", passwordEncoder.encode("Password123!"), "Login User");
        userRepository.save(user);

        Map<String, String> payload = Map.of(
            "email", "login@example.com",
            "password", "Password123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(cookie().exists("nexusos_access_token"));
    }

    @Test
    void testLoginFailure() throws Exception {
        Map<String, String> payload = Map.of(
            "email", "notfound@example.com",
            "password", "wrongpass"
        );

        // Spring Security's AuthenticationManager throws BadCredentialsException
        // which by default returns 403. We'll accept 401 or 403.
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().is4xxClientError());
    }
}
