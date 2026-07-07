package com.company.wiki.mail.service;

import com.company.wiki.common.util.AesEncryptUtil;
import com.company.wiki.mail.entity.MailAccount;
import com.company.wiki.mail.entity.MailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
                    String from = decodeSender(msg);
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

    private String decodeSender(Message msg) {
        try {
            Address[] from = msg.getFrom();
            if (from == null || from.length == 0) return "";
            Address addr = from[0];
            if (addr instanceof InternetAddress ia) {
                String personal = ia.getPersonal();
                String email = ia.getAddress();
                if (personal != null && !personal.isBlank()) {
                    try { personal = MimeUtility.decodeText(personal); } catch (Exception ignore) {}
                    return personal + " <" + email + ">";
                }
                return email != null ? email : addr.toString();
            }
            return MimeUtility.decodeText(addr.toString());
        } catch (Exception e) {
            log.warn("발신자 디코딩 실패: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 메일 파트에서 텍스트 본문을 재귀적으로 추출한다.
     * text/plain 우선, 없으면 text/html 태그 제거 후 반환.
     */
    private String extractText(Part part) throws MessagingException {
        try {
            if (part.isMimeType("text/plain")) {
                return readPartAsString(part);
            }
            if (part.isMimeType("text/html")) {
                return stripHtml(readPartAsString(part));
            }
            if (part.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) part.getContent();
                String plainText = null;
                String htmlText = null;
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);
                    if (bp.isMimeType("text/plain") && plainText == null) {
                        try { plainText = extractText(bp); } catch (Exception ignore) {}
                    } else if (bp.isMimeType("text/html") && htmlText == null) {
                        try { htmlText = extractText(bp); } catch (Exception ignore) {}
                    } else if (bp.isMimeType("multipart/*")) {
                        try {
                            String nested = extractText(bp);
                            if (nested != null && !nested.isBlank() && plainText == null) {
                                plainText = nested;
                            }
                        } catch (Exception ignore) {}
                    }
                }
                if (plainText != null && !plainText.isBlank()) return plainText;
                if (htmlText != null && !htmlText.isBlank()) return htmlText;
            }
        } catch (IOException e) {
            log.warn("본문 추출 중 IO 오류: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Part 내용을 String으로 읽는다.
     * getContent()가 InputStream을 반환하는 경우(base64/qp 등) 직접 읽는다.
     */
    private String readPartAsString(Part part) throws MessagingException, IOException {
        Object content = part.getContent();
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof InputStream is) {
            Charset charset = detectCharset(part.getContentType());
            byte[] bytes = is.readAllBytes();
            return new String(bytes, charset);
        }
        return content != null ? content.toString() : "";
    }

    private Charset detectCharset(String contentType) {
        if (contentType != null) {
            for (String token : contentType.split(";")) {
                String t = token.trim();
                if (t.toLowerCase().startsWith("charset=")) {
                    String name = t.substring(8).trim().replace("\"", "");
                    try { return Charset.forName(name); } catch (Exception ignore) {}
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html
                .replaceAll("(?si)<style[^>]*>.*?</style>", "")
                .replaceAll("(?si)<script[^>]*>.*?</script>", "")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("(\\s*\\n){3,}", "\n\n")
                .trim();
    }
}
