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
class GroupControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    void createGroup_asAdmin_returns200() throws Exception {
        String token = getAdminToken();

        Map<String, String> req = Map.of(
                "name", "개발팀",
                "description", "개발자 그룹"
        );

        mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("개발팀"));
    }

    @Test
    void addMember_asAdmin_returns200() throws Exception {
        String token = getAdminToken();

        // 그룹 생성
        Map<String, String> groupReq = Map.of("name", "테스트그룹", "description", "테스트");
        MvcResult groupResult = mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupReq)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> groupResp = objectMapper.readValue(groupResult.getResponse().getContentAsString(), Map.class);
        Map<?, ?> groupData = (Map<?, ?>) groupResp.get("data");
        Long groupId = ((Number) groupData.get("id")).longValue();

        // 사용자 생성
        Map<String, String> userReq = Map.of(
                "loginId", "groupmember01",
                "name", "그룹 멤버",
                "email", "groupmember01@co.com",
                "password", "Pass1234!"
        );
        MvcResult userResult = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> userResp = objectMapper.readValue(userResult.getResponse().getContentAsString(), Map.class);
        Map<?, ?> userData = (Map<?, ?>) userResp.get("data");
        Long userId = ((Number) userData.get("id")).longValue();

        // 멤버 추가
        Map<String, Long> memberReq = Map.of("userId", userId);
        mockMvc.perform(post("/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getGroups_withoutToken_returns401() throws Exception {
        // groups 목록은 authenticated 사용자만 조회 가능
        mockMvc.perform(get("/groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGroup_withoutToken_returns401() throws Exception {
        Map<String, String> req = Map.of("name", "미인증그룹");
        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
