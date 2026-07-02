package com.company.wiki.user.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    /**
     * admin 토큰 획득 헬퍼 메서드
     */
    private String getAdminToken() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin", "Admin1234!");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Map<?, ?> resp = objectMapper.readValue(body, Map.class);
        Map<?, ?> data = (Map<?, ?>) resp.get("data");
        return (String) data.get("accessToken");
    }

    @Test
    void createUser_asAdmin_returns200() throws Exception {
        String token = getAdminToken();

        Map<String, String> req = Map.of(
                "loginId", "testuser01",
                "name", "테스트 사용자",
                "email", "testuser01@co.com",
                "password", "Pass1234!"
        );

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.loginId").value("testuser01"));
    }

    @Test
    void getUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserList_asAdmin_returns200() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void updateUser_asAdmin_returns200() throws Exception {
        String token = getAdminToken();

        // 먼저 사용자 생성
        Map<String, String> createReq = Map.of(
                "loginId", "testuser02",
                "name", "수정 전 이름",
                "email", "testuser02@co.com",
                "password", "Pass1234!"
        );

        MvcResult createResult = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> createResp = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Map<?, ?> createdData = (Map<?, ?>) createResp.get("data");
        Long userId = ((Number) createdData.get("id")).longValue();

        // 사용자 수정
        Map<String, String> updateReq = Map.of("name", "수정 후 이름");
        mockMvc.perform(put("/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정 후 이름"));
    }

    @Test
    void deleteUser_asAdmin_returns200() throws Exception {
        String token = getAdminToken();

        // 먼저 사용자 생성
        Map<String, String> createReq = Map.of(
                "loginId", "testuser03",
                "name", "삭제 대상",
                "email", "testuser03@co.com",
                "password", "Pass1234!"
        );

        MvcResult createResult = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> createResp = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Map<?, ?> createdData = (Map<?, ?>) createResp.get("data");
        Long userId = ((Number) createdData.get("id")).longValue();

        // 비활성화
        mockMvc.perform(delete("/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
