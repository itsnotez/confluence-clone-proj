package com.company.wiki.mail.controller;

import com.company.wiki.auth.dto.LoginRequest;
import com.company.wiki.mail.entity.MailMessage;
import com.company.wiki.mail.repository.MailMessageRepository;
import com.company.wiki.space.dto.SpaceDto;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class MailMessageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MailMessageRepository mailMessageRepository;

    // ── 헬퍼 ───────────────────────────────────────────────────────────────────

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

    private Long setupSpaceAndAccount(String token) throws Exception {
        // Space 생성 (이미 있으면 무시)
        SpaceDto.CreateRequest spaceReq = SpaceDto.CreateRequest.builder()
                .spaceKey("MSG")
                .name("메시지 테스트")
                .type("PRIVATE")
                .build();
        mockMvc.perform(post("/spaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(spaceReq)));

        // MailAccount 생성
        String accountBody = "{\"emailAddress\":\"test@msg.com\",\"password\":\"pass\",\"imapHost\":\"imap.msg.com\"}";
        MvcResult result = mockMvc.perform(post("/spaces/MSG/mail-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private Long insertTestMessage(Long accountId) {
        MailMessage msg = MailMessage.builder()
                .mailAccountId(accountId)
                .messageUid("test-uid-" + System.currentTimeMillis())
                .subject("테스트 제목")
                .sender("sender@test.com")
                .recipients("recipient@test.com")
                .receivedAt(LocalDateTime.now())
                .bodyText("본문 내용입니다.")
                .status("UNREAD")
                .build();
        return mailMessageRepository.save(msg).getId();
    }

    // ── 테스트 ─────────────────────────────────────────────────────────────────

    @Test
    void getMessages_emptyList() throws Exception {
        String token = adminToken();
        Long accountId = setupSpaceAndAccount(token);

        mockMvc.perform(get("/spaces/MSG/mail-accounts/" + accountId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getMessages_returnsSavedMessage() throws Exception {
        String token = adminToken();
        Long accountId = setupSpaceAndAccount(token);
        insertTestMessage(accountId);

        mockMvc.perform(get("/spaces/MSG/mail-accounts/" + accountId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void convertToPage_success() throws Exception {
        String token = adminToken();
        Long accountId = setupSpaceAndAccount(token);
        Long msgId = insertTestMessage(accountId);

        mockMvc.perform(post("/spaces/MSG/mail-accounts/" + accountId + "/messages/" + msgId + "/convert")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contentId").isNotEmpty())
                .andExpect(jsonPath("$.data.message").value("메일이 페이지로 변환되었습니다"));
    }

    @Test
    void convertToPage_alreadyConverted_returns409() throws Exception {
        String token = adminToken();
        Long accountId = setupSpaceAndAccount(token);
        Long msgId = insertTestMessage(accountId);

        // 첫 번째 변환 — 성공
        mockMvc.perform(post("/spaces/MSG/mail-accounts/" + accountId + "/messages/" + msgId + "/convert")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // 두 번째 변환 — 409 CONFLICT
        mockMvc.perform(post("/spaces/MSG/mail-accounts/" + accountId + "/messages/" + msgId + "/convert")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }
}
