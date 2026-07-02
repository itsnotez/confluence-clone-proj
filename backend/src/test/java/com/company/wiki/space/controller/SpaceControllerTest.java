package com.company.wiki.space.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.space.dto.SpaceDto;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class SpaceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
    }

    private String adminToken() throws Exception {
        LoginRequest req = new LoginRequest("admin", "Admin1234!");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body)
                .path("data")
                .path("accessToken")
                .asText();
    }

    private void createTestSpace(String spaceKey) throws Exception {
        SpaceDto.CreateRequest req = SpaceDto.CreateRequest.builder()
                .spaceKey(spaceKey)
                .name("테스트 Space")
                .type("PRIVATE")
                .build();

        mockMvc.perform(post("/spaces")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void createSpace_success() throws Exception {
        SpaceDto.CreateRequest req = SpaceDto.CreateRequest.builder()
                .spaceKey("TEST")
                .name("테스트 Space")
                .type("PRIVATE")
                .build();

        mockMvc.perform(post("/spaces")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.spaceKey").value("TEST"));
    }

    @Test
    void createSpace_duplicateKey_returns409() throws Exception {
        createTestSpace("DUPKEY");

        SpaceDto.CreateRequest req = SpaceDto.CreateRequest.builder()
                .spaceKey("DUPKEY")
                .name("중복 Space")
                .type("PRIVATE")
                .build();

        mockMvc.perform(post("/spaces")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void getSpaces_returnsActivelist() throws Exception {
        createTestSpace("LISTTEST");

        mockMvc.perform(get("/spaces")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void deleteSpace_softDelete() throws Exception {
        createTestSpace("DELTEST");

        mockMvc.perform(delete("/spaces/DELTEST")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/spaces/DELTEST")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleFavorite_addsAndRemoves() throws Exception {
        createTestSpace("FAVTEST");

        // 즐겨찾기 추가
        mockMvc.perform(post("/spaces/FAVTEST/favorite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 즐겨찾기 제거 (두 번째 토글)
        mockMvc.perform(post("/spaces/FAVTEST/favorite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
