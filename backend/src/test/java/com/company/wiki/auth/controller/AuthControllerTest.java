package com.company.wiki.auth.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성 (트랜잭션 롤백으로 테스트 간 독립성 보장)
        if (!userRepository.existsByLoginId("testuser")) {
            userRepository.save(User.builder()
                    .loginId("testuser")
                    .name("테스트 사용자")
                    .email("testuser@company.com")
                    .password(passwordEncoder.encode("Test1234!"))
                    .role("MEMBER")
                    .status("ACTIVE")
                    .build());
        }
    }

    @Test
    void login_withValidCredentials_returnsTokens() throws Exception {
        LoginRequest req = new LoginRequest("testuser", "Test1234!");
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("MEMBER"));
    }

    @Test
    void login_withInvalidPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest("testuser", "wrongpassword");
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withMissingField_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
