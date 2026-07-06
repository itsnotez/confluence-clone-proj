package com.company.wiki.mail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "mail_messages",
    uniqueConstraints = @UniqueConstraint(columnNames = {"mail_account_id", "message_uid"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mail_account_id", nullable = false)
    private Long mailAccountId;

    @Column(name = "message_uid", length = 500, nullable = false)
    private String messageUid;

    @Column(name = "thread_id", length = 500)
    private String threadId;

    @Column(name = "subject", length = 1000)
    private String subject;

    @Column(name = "sender", length = 500)
    private String sender;

    @Column(name = "recipients", columnDefinition = "TEXT")
    private String recipients;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "linked_content_id")
    private Long linkedContentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "UNREAD";
        }
    }
}
