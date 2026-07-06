package com.company.wiki.search.controller;

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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class SearchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String adminToken;
    private String uniqueKeyword;
    private Long createdContentId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        uniqueKeyword = "srchkw" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        createTestSpaceAndContent(adminToken, uniqueKeyword);
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

    private void createTestSpaceAndContent(String token, String keyword) throws Exception {
        // Space 생성
        Map<String, String> spaceReq = Map.of(
                "spaceKey", "SRCH",
                "name", "검색 테스트 Space",
                "type", "PRIVATE"
        );
        mockMvc.perform(post("/spaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(spaceReq)));

        // Content 생성 (제목에 고유 키워드 포함 — contents.search_vector STORED 컬럼이 제목 기반)
        Map<String, Object> contentReq = new java.util.HashMap<>();
        contentReq.put("title", keyword + " 테스트 페이지");
        contentReq.put("body", "검색 테스트 본문입니다.");

        MvcResult result = mockMvc.perform(post("/spaces/SRCH/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contentReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        createdContentId = data.path("id").asLong();
    }

    @Test
    void search_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "테스트")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void search_findsCreatedContent() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", uniqueKeyword)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + createdContentId + ")]").exists());
    }

    @Test
    void search_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "x"))
                .andExpect(status().isUnauthorized());
    }
}
