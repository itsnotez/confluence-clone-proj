package com.company.wiki.content.controller;

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
class ContentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        createTestSpace(adminToken);
    }

    private String adminToken() throws Exception {
        LoginRequest req = new LoginRequest("admin", "Admin1234!");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    private void createTestSpace(String token) throws Exception {
        Map<String, String> req = Map.of(
                "spaceKey", "TEST",
                "name", "테스트 Space",
                "type", "PRIVATE"
        );
        mockMvc.perform(post("/spaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private Long createContent(String token, String title, String body) throws Exception {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("title", title);
        req.put("body", body);

        MvcResult result = mockMvc.perform(post("/spaces/TEST/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        return data.path("id").asLong();
    }

    @Test
    void createContent_success() throws Exception {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("title", "첫번째 페이지");
        req.put("body", "{}");

        mockMvc.perform(post("/spaces/TEST/contents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void getContentTree_returnsTree() throws Exception {
        createContent(adminToken, "트리 테스트 페이지", "{}");

        mockMvc.perform(get("/spaces/TEST/contents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void publishContent_incrementsVersion() throws Exception {
        Long contentId = createContent(adminToken, "게시 테스트", "{}");

        Map<String, String> publishReq = Map.of("body", "{}");
        mockMvc.perform(post("/contents/" + contentId + "/publish")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void getVersions_afterPublish() throws Exception {
        Long contentId = createContent(adminToken, "버전 테스트", "{}");

        Map<String, String> publishReq = Map.of("body", "{}");
        mockMvc.perform(post("/contents/" + contentId + "/publish")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/contents/" + contentId + "/versions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].versionNo").value(2));
    }

    @Test
    void deleteContent_softDelete() throws Exception {
        Long contentId = createContent(adminToken, "삭제 테스트", "{}");

        mockMvc.perform(delete("/contents/" + contentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/contents/" + contentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
