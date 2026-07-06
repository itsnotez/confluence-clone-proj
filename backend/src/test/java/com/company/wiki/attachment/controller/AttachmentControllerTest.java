package com.company.wiki.attachment.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AttachmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String adminToken;
    private Long contentId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        createTestSpace(adminToken);
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

    private void createTestSpace(String token) throws Exception {
        Map<String, String> req = Map.of(
                "spaceKey", "ATTEST",
                "name", "Attachment Test Space",
                "type", "PRIVATE"
        );
        mockMvc.perform(post("/spaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private Long createTestContent(String token) throws Exception {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("title", "첨부파일 테스트 페이지");
        req.put("body", "내용");
        MvcResult result = mockMvc.perform(post("/spaces/ATTEST/contents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void uploadFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello attachment".getBytes());

        mockMvc.perform(multipart("/contents/" + contentId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"));
    }

    @Test
    void downloadFile_success() throws Exception {
        byte[] content = "download content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "download.txt", "text/plain", content);

        MvcResult uploadResult = mockMvc.perform(multipart("/contents/" + contentId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        Long attachmentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult downloadResult = mockMvc.perform(get("/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();

        byte[] responseBytes = downloadResult.getResponse().getContentAsByteArray();
        assert responseBytes.length == content.length;
    }

    @Test
    void listAttachments_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "list.txt", "text/plain", "list test".getBytes());

        mockMvc.perform(multipart("/contents/" + contentId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/contents/" + contentId + "/attachments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
