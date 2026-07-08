package com.company.wiki.mail.service;

import com.company.wiki.mail.entity.MailAccount;
import com.company.wiki.mail.entity.MailMessage;
import com.company.wiki.mail.repository.MailAccountRepository;
import com.company.wiki.mail.repository.MailMessageAttachmentRepository;
import com.company.wiki.mail.repository.MailMessageRepository;
import com.company.wiki.mail.service.ImapService.FetchedMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailSyncServiceTest {

    @Mock
    private ImapService imapService;

    @Mock
    private MailAccountRepository mailAccountRepository;

    @Mock
    private MailMessageRepository mailMessageRepository;

    @Mock
    private MailMessageAttachmentRepository mailMessageAttachmentRepository;

    @InjectMocks
    private MailSyncService mailSyncService;

    /**
     * 테스트용 MailAccount 헬퍼
     */
    private MailAccount testAccount() {
        return MailAccount.builder()
                .id(1L)
                .emailAddress("t@t.com")
                .syncStatus("ACTIVE")
                .credential("enc")
                .build();
    }

    private MailMessage buildMessage(String uid) {
        return MailMessage.builder()
                .mailAccountId(1L)
                .messageUid(uid)
                .subject("테스트 메일")
                .build();
    }

    /**
     * 테스트 1: 신규 메시지 2건이 모두 저장된다.
     */
    @Test
    @DisplayName("syncAccount_newMessages_savedSuccessfully: 신규 메시지 2건 저장 확인")
    void syncAccount_newMessages_savedSuccessfully() {
        // given
        MailAccount account = testAccount();
        MailMessage msg1 = buildMessage("1");
        MailMessage msg2 = buildMessage("2");

        when(imapService.fetchNewMessages(account, 50)).thenReturn(
                List.of(new FetchedMessage(msg1, List.of()), new FetchedMessage(msg2, List.of())));
        when(mailMessageRepository.findByMailAccountIdAndMessageUid(1L, "1")).thenReturn(Optional.empty());
        when(mailMessageRepository.findByMailAccountIdAndMessageUid(1L, "2")).thenReturn(Optional.empty());
        when(mailMessageRepository.save(any(MailMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        mailSyncService.syncAccount(account);

        // then
        verify(mailMessageRepository, times(2)).save(any(MailMessage.class));
        assertThat(account.getSyncStatus()).isEqualTo("ACTIVE");
    }

    /**
     * 테스트 2: 이미 존재하는 메시지(uid="1")는 저장하지 않는다.
     */
    @Test
    @DisplayName("syncAccount_duplicateMessage_skipped: 중복 메시지 건너뜀")
    void syncAccount_duplicateMessage_skipped() {
        // given
        MailAccount account = testAccount();
        MailMessage msg1 = buildMessage("1");
        MailMessage existing = buildMessage("1");
        existing.setBodyText("기존 본문");

        when(imapService.fetchNewMessages(account, 50)).thenReturn(
                List.of(new FetchedMessage(msg1, List.of())));
        when(mailMessageRepository.findByMailAccountIdAndMessageUid(1L, "1")).thenReturn(Optional.of(existing));

        // when
        mailSyncService.syncAccount(account);

        // then
        verify(mailMessageRepository, never()).save(any(MailMessage.class));
    }

    /**
     * 테스트 3: IMAP fetch가 3번 연속 실패하면 syncStatus가 DISABLED로 변경된다.
     */
    @Test
    @DisplayName("syncAccount_imapFailure_3retries_disabled: 3회 실패 시 DISABLED")
    void syncAccount_imapFailure_3retries_disabled() {
        // given
        MailAccount account = testAccount();

        when(imapService.fetchNewMessages(account, 50))
                .thenThrow(new RuntimeException("IMAP error"));

        // when
        mailSyncService.syncAccount(account);

        // then
        assertThat(account.getSyncStatus()).isEqualTo("DISABLED");
        verify(mailAccountRepository, times(3)).save(account);
    }
}
