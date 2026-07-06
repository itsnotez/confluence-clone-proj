package com.company.wiki.mail.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.mail.dto.MailAccountDto;
import com.company.wiki.mail.repository.MailAccountRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class MailAccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MailAccountRepository mailAccountRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        createSpace(adminToken);
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

    private void createSpace(String token) throws Exception {
        SpaceDto.CreateRequest req = SpaceDto.CreateRequest.builder()
                .spaceKey("MAIL")
                .name("메일 테스트")
                .type("PRIVATE")
                .build();

        mockMvc.perform(post("/spaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn(); // already created is ok due to @Transactional
    }

    private MailAccountDto.CreateRequest buildCreateRequest() {
        return MailAccountDto.CreateRequest.builder()
                .emailAddress("test@test.com")
                .password("pass")
                .imapHost("imap.test.com")
                .build();
    }

    @Test
    void createMailAccount_success() throws Exception {
        MailAccountDto.CreateRequest req = buildCreateRequest();

        mockMvc.perform(post("/spaces/MAIL/mail-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emailAddress").value("test@test.com"));
    }

    @Test
    void getMailAccounts_returnsList() throws Exception {
        // 먼저 생성
        MailAccountDto.CreateRequest req = buildCreateRequest();
        mockMvc.perform(post("/spaces/MAIL/mail-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // 조회
        mockMvc.perform(get("/spaces/MAIL/mail-accounts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void deleteMailAccount_success() throws Exception {
        // 생성
        MailAccountDto.CreateRequest req = buildCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/spaces/MAIL/mail-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        // 삭제
        mockMvc.perform(delete("/spaces/MAIL/mail-accounts/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void credential_isEncrypted() throws Exception {
        MailAccountDto.CreateRequest req = buildCreateRequest();
        MvcResult createResult = mockMvc.perform(post("/spaces/MAIL/mail-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        // DB에서 직접 조회하여 credential이 평문 "pass"가 아닌지 확인
        String credential = mailAccountRepository.findById(id)
                .orElseThrow()
                .getCredential();

        assertThat(credential).isNotEqualTo("pass");
        assertThat(credential).isNotBlank();
    }
}
