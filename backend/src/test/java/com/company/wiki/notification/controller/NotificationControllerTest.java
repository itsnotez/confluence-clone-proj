package com.company.wiki.notification.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.notification.entity.Notification;
import com.company.wiki.notification.repository.NotificationRepository;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String adminToken;
    private Long adminUserId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();
        // admin 사용자의 userId를 JWT에서 추출 (admin은 시드 데이터에 의해 id=1)
        adminUserId = getAdminUserId();
    }

    private String getAdminToken() throws Exception {
        LoginRequest req = new LoginRequest("admin", "Admin1234!");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long getAdminUserId() {
        return userRepository.findByLoginId("admin")
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("admin user not found"));
    }

    private Long createOtherUser() {
        User otherUser = userRepository.save(User.builder()
                .loginId("notif_other_user")
                .name("다른 사용자")
                .email("notif_other@company.com")
                .password(passwordEncoder.encode("Other1234!"))
                .role("MEMBER")
                .status("ACTIVE")
                .build());
        return otherUser.getId();
    }

    @Test
    void getNotifications_returns200() throws Exception {
        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unreadCount_returns200() throws Exception {
        MvcResult result = mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        // data should be a number (long)
        org.junit.jupiter.api.Assertions.assertTrue(data.isNumber(), "data should be a number, got: " + data);
    }

    @Test
    void markRead_returns200() throws Exception {
        Notification notification = notificationRepository.save(Notification.builder()
                .userId(adminUserId)
                .type("COMMENT")
                .title("테스트 알림")
                .message("테스트 메시지")
                .linkUrl("/contents/1")
                .build());

        mockMvc.perform(patch("/notifications/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void markRead_otherUser_returns404() throws Exception {
        // Create a notification for another real user
        Long otherUserId = createOtherUser();

        Notification notification = notificationRepository.save(Notification.builder()
                .userId(otherUserId)
                .type("COMMENT")
                .title("다른 사용자 알림")
                .message("다른 사용자 메시지")
                .linkUrl("/contents/1")
                .build());

        // Admin tries to mark another user's notification as read → should get 404 (IDOR prevention)
        mockMvc.perform(patch("/notifications/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllRead_returns200() throws Exception {
        mockMvc.perform(patch("/notifications/read-all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
