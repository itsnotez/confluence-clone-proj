package com.company.wiki.mail.dto;

import com.company.wiki.mail.entity.MailMessage;
import lombok.*;

import java.time.LocalDateTime;

public class MailMessageDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private Long mailAccountId;
        private String messageUid;
        private String subject;
        private String sender;
        private String recipients;
        private LocalDateTime receivedAt;
        private String bodyPreview;
        private String status;
        private Long linkedContentId;
        private LocalDateTime createdAt;
        private boolean hasAttachment;

        public static Response from(MailMessage msg) {
            String preview = null;
            if (msg.getBodyText() != null) {
                preview = msg.getBodyText().length() > 500
                        ? msg.getBodyText().substring(0, 500)
                        : msg.getBodyText();
            }
            return Response.builder()
                    .id(msg.getId())
                    .mailAccountId(msg.getMailAccountId())
                    .messageUid(msg.getMessageUid())
                    .subject(msg.getSubject())
                    .sender(msg.getSender())
                    .recipients(msg.getRecipients())
                    .receivedAt(msg.getReceivedAt())
                    .bodyPreview(preview)
                    .status(msg.getStatus())
                    .linkedContentId(msg.getLinkedContentId())
                    .createdAt(msg.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConvertRequest {
        // 제목/내용은 메시지에서 자동 추출 — 빈 body
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConvertResponse {
        private Long contentId;
        private String contentTitle;
        private String message;
    }
}
