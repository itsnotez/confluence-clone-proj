package com.company.wiki.mail.service;

import com.company.wiki.common.util.AesEncryptUtil;
import com.company.wiki.mail.entity.MailAccount;
import com.company.wiki.mail.entity.MailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImapService {

    private final AesEncryptUtil aesEncryptUtil;

    /**
     * 지정된 IMAP 계정에서 최근 maxFetch건의 메시지를 fetch한다.
     *
     * @param account  IMAP 계정 정보 (자격증명 암호화됨)
     * @param maxFetch 최대 가져올 메시지 수
     * @return fetch된 MailMessage 목록 (DB 미저장 상태)
     */
    public List<MailMessage> fetchNewMessages(MailAccount account, int maxFetch) {
        String password = aesEncryptUtil.decrypt(account.getCredential());

        Properties props = new Properties();
        String protocol = account.isImapSsl() ? "imaps" : "imap";
        props.put("mail.store.protocol", protocol);
        props.put("mail.imap.host", account.getImapHost());
        props.put("mail.imap.port", String.valueOf(account.getImapPort()));
        props.put("mail.imap.ssl.enable", String.valueOf(account.isImapSsl()));
        props.put("mail.imap.connectiontimeout", "5000");
        props.put("mail.imap.timeout", "5000");
        props.put("mail.imaps.host", account.getImapHost());
        props.put("mail.imaps.port", String.valueOf(account.getImapPort()));
        props.put("mail.imaps.connectiontimeout", "5000");
        props.put("mail.imaps.timeout", "5000");

        Session session = Session.getInstance(props);
        Store store = null;
        Folder inbox = null;

        try {
            store = session.getStore(protocol);
            store.connect(account.getImapHost(), account.getEmailAddress(), password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            if (messages.length == 0) {
                return new ArrayList<>();
            }

            int start = Math.max(1, messages.length - maxFetch + 1);
            Message[] recent = inbox.getMessages(start, messages.length);

            List<MailMessage> result = new ArrayList<>();
            for (Message msg : recent) {
                try {
                    String uid = String.valueOf(((UIDFolder) inbox).getUID(msg));
                    String subject = msg.getSubject() != null ? msg.getSubject() : "(제목없음)";
                    String from = msg.getFrom() != null && msg.getFrom().length > 0
                            ? msg.getFrom()[0].toString() : "";
                    LocalDateTime received = msg.getReceivedDate() != null
                            ? msg.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : LocalDateTime.now();
                    String body = extractText(msg);

                    result.add(MailMessage.builder()
                            .mailAccountId(account.getId())
                            .messageUid(uid)
                            .subject(subject)
                            .sender(from)
                            .receivedAt(received)
                            .bodyText(body)
                            .build());
                } catch (Exception e) {
                    log.warn("메시지 파싱 실패 (건너뜀): {}", e.getMessage());
                }
            }

            return result;
        } catch (MessagingException e) {
            log.error("IMAP fetch 실패 — 계정: {}, 오류: {}", account.getEmailAddress(), e.getMessage());
            throw new RuntimeException("IMAP 메시지 fetch 실패: " + e.getMessage(), e);
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                }
            } catch (MessagingException ignore) {
                // 닫기 실패는 무시
            }
            try {
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException ignore) {
                // 닫기 실패는 무시
            }
        }
    }

    /**
     * 메일 파트에서 텍스트 본문을 재귀적으로 추출한다.
     */
    private String extractText(Part part) throws MessagingException {
        try {
            if (part.isMimeType("text/plain")) {
                return (String) part.getContent();
            }
            if (part.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) part.getContent();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mp.getCount(); i++) {
                    String text = extractText(mp.getBodyPart(i));
                    if (text != null && !text.isEmpty()) {
                        sb.append(text);
                    }
                }
                return sb.toString();
            }
        } catch (IOException e) {
            log.warn("본문 추출 중 IO 오류: {}", e.getMessage());
        }
        return "";
    }
}
