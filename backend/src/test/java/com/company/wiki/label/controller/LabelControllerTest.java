package com.company.wiki.label.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class LabelControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String adminToken;
    private Long spaceId;
    private Long contentId;
    private Long labelId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        spaceId = createSpace(adminToken);
        contentId = createContent(adminToken, spaceId);
        labelId = createLabel(adminToken, spaceId, "긴급", "#f00");
    }

    private String adminToken() throws Exception {
        LoginRequest req = new LoginRequest("admin", "Admin1234!");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long createSpace(String token) throws Exception {
        Map<String, String> req = Map.of(
                "spaceKey", "LBLTEST",
                "name", "라벨 테스트 Space",
                "type", "PRIVATE"
        );
        MvcResult result = mockMvc.perform(post("/spaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createContent(String token, Long sid) throws Exception {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("title", "라벨 테스트 페이지");
        req.put("body", "{}");
        // space key로 생성
        MvcResult result = mockMvc.perform(post("/spaces/LBLTEST/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long createLabel(String token, Long sid, String name, String color) throws Exception {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("spaceId", sid);
        req.put("name", name);
        req.put("color", color);
        MvcResult result = mockMvc.perform(post("/labels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void addLabel_success() throws Exception {
        Map<String, Long> req = Map.of("labelId", labelId);
        mockMvc.perform(post("/contents/" + contentId + "/labels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getLabels_success() throws Exception {
        // 먼저 라벨 추가
        Map<String, Long> addReq = Map.of("labelId", labelId);
        mockMvc.perform(post("/contents/" + contentId + "/labels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        // 라벨 목록 조회
        MvcResult result = mockMvc.perform(get("/contents/" + contentId + "/labels")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        boolean found = false;
        for (JsonNode node : data) {
            if ("긴급".equals(node.path("name").asText())) {
                found = true;
                break;
            }
        }
        assert found : "추가한 라벨 '긴급'이 조회 결과에 없습니다";
    }

    @Test
    void getLabels_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/contents/" + contentId + "/labels"))
                .andExpect(status().isUnauthorized());
    }
}
