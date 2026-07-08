package com.company.wiki.mail.service;

import com.company.wiki.mail.entity.MailAccount;
import com.company.wiki.mail.entity.MailMessage;
import com.company.wiki.mail.entity.MailMessageAttachment;
import com.company.wiki.mail.repository.MailAccountRepository;
import com.company.wiki.mail.repository.MailMessageAttachmentRepository;
import com.company.wiki.mail.repository.MailMessageRepository;
import com.company.wiki.mail.service.ImapService.FetchedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailSyncService {

    private final ImapService imapService;
    private final MailAccountRepository mailAccountRepository;
    private final MailMessageRepository mailMessageRepository;
    private final MailMessageAttachmentRepository mailMessageAttachmentRepository;

    /**
     * 단일 메일 계정의 신규 메시지를 동기화한다.
     * 3회 연속 실패 시 syncStatus를 DISABLED로 변경한다.
     *
     * @param account 동기화할 메일 계정
     */
    @Transactional
    public void syncAccount(MailAccount account) {
        if ("DISABLED".equals(account.getSyncStatus())) {
            log.info("계정 {} 은 DISABLED 상태 — 동기화 건너뜀", account.getEmailAddress());
            return;
        }

        int retries = 0;
        while (retries < 3) {
            try {
                List<FetchedMessage> fetched = imapService.fetchNewMessages(account, 50);
                int saved = 0;
                for (FetchedMessage fetched1 : fetched) {
                    MailMessage msg = fetched1.message();
                    List<MailMessageAttachment> attachments = fetched1.attachments();
                    mailMessageRepository
                            .findByMailAccountIdAndMessageUid(account.getId(), msg.getMessageUid())
                            .ifPresentOrElse(existing -> {
                                // 본문이 비어있던 기존 메시지는 내용 업데이트
                                if (existing.getBodyText() == null || existing.getBodyText().isBlank()) {
                                    existing.setBodyText(msg.getBodyText());
                                    existing.setSender(msg.getSender());
                                    existing.setSubject(msg.getSubject());
                                    mailMessageRepository.save(existing);
                                    // 첨부파일이 아직 없는 경우에만 저장 (중복 방지)
                                    if (!attachments.isEmpty()) {
                                        List<MailMessageAttachment> existingAttachments =
                                                mailMessageAttachmentRepository.findByMailMessageId(existing.getId());
                                        if (existingAttachments.isEmpty()) {
                                            attachments.forEach(a -> a.setMailMessageId(existing.getId()));
                                            mailMessageAttachmentRepository.saveAll(attachments);
                                        }
                                    }
                                }
                            }, () -> {
                                MailMessage savedMsg = mailMessageRepository.save(msg);
                                if (!attachments.isEmpty()) {
                                    attachments.forEach(a -> a.setMailMessageId(savedMsg.getId()));
                                    mailMessageAttachmentRepository.saveAll(attachments);
                                }
                            });
                    saved++;
                }
                account.setSyncStatus("ACTIVE");
                account.setLastSyncedAt(LocalDateTime.now());
                account.setLastErrorMessage(null);
                mailAccountRepository.save(account);
                log.info("계정 {} 동기화 완료: {}건 저장", account.getEmailAddress(), saved);
                return;
            } catch (Exception e) {
                retries++;
                String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                log.error("동기화 실패 ({}/3) — 계정: {}, 오류: {}",
                        retries, account.getEmailAddress(), errorMsg);
                if (retries >= 3) {
                    account.setSyncStatus("DISABLED");
                } else {
                    account.setSyncStatus("ERROR");
                }
                account.setLastErrorMessage(errorMsg);
                mailAccountRepository.save(account);
            }
        }
    }

    /**
     * ACTIVE, PENDING, ERROR 상태의 모든 메일 계정을 동기화한다.
     */
    public void syncAll() {
        List<MailAccount> targets = new ArrayList<>();
        targets.addAll(mailAccountRepository.findBySyncStatus("ACTIVE"));
        targets.addAll(mailAccountRepository.findBySyncStatus("PENDING"));
        targets.addAll(mailAccountRepository.findBySyncStatus("ERROR"));
        log.info("동기화 대상 계정: {}개", targets.size());
        targets.forEach(this::syncAccount);
    }
}
