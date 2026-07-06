package com.company.wiki.comment.controller;

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
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String adminToken;
    private Long contentId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        contentId = createTestContent(adminToken);
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

    private Long createTestContent(String token) throws Exception {
        // Space 생성
        Map<String, String> spaceReq = Map.of(
                "spaceKey", "CMTTEST",
                "name", "댓글 테스트 Space",
                "type", "PRIVATE"
        );
        mockMvc.perform(post("/spaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(spaceReq)));

        // Content 생성
        Map<String, Object> contentReq = new java.util.HashMap<>();
        contentReq.put("title", "댓글 테스트 페이지");
        contentReq.put("body", "{}");

        MvcResult result = mockMvc.perform(post("/spaces/CMTTEST/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contentReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asLong();
    }

    @Test
    void createComment_success() throws Exception {
        Map<String, Object> req = Map.of("body", "댓글1");

        mockMvc.perform(post("/contents/" + contentId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void deleteOwnComment_success() throws Exception {
        // 댓글 작성
        Map<String, Object> createReq = Map.of("body", "삭제할 댓글");
        MvcResult createResult = mockMvc.perform(post("/contents/" + contentId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();
        Long commentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 댓글 삭제
        mockMvc.perform(delete("/comments/" + commentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // soft delete 확인: 목록에 없어야 함
        MvcResult listResult = mockMvc.perform(get("/contents/" + contentId + "/comments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("data");
        boolean found = false;
        for (JsonNode node : data) {
            if (node.path("id").asLong() == commentId) {
                found = true;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertFalse(found, "soft delete된 댓글이 목록에 노출됨");
    }

    @Test
    void getComments_returnsTree() throws Exception {
        // 루트 댓글 작성
        Map<String, Object> rootReq = Map.of("body", "루트 댓글");
        MvcResult rootResult = mockMvc.perform(post("/contents/" + contentId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rootReq)))
                .andExpect(status().isOk())
                .andReturn();
        Long rootId = objectMapper.readTree(rootResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 대댓글 작성
        Map<String, Object> childReq = Map.of("body", "대댓글", "parentCommentId", rootId);
        mockMvc.perform(post("/contents/" + contentId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isOk());

        // 트리 조회
        mockMvc.perform(get("/contents/" + contentId + "/comments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].children").isArray())
                .andExpect(jsonPath("$.data[0].children[0].body").value("대댓글"));
    }
}
