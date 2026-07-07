package com.company.wiki.mail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "email_address", length = 300)
    private String emailAddress;

    @Column(name = "imap_host", length = 500)
    private String imapHost;

    @Column(name = "imap_port")
    private int imapPort;

    @Column(name = "imap_ssl")
    private boolean imapSsl;

    @Column(name = "smtp_host", length = 500)
    private String smtpHost;

    @Column(name = "smtp_port")
    private int smtpPort;

    @Column(name = "credential", columnDefinition = "TEXT", nullable = false)
    private String credential;

    @Column(name = "sync_status", length = 20)
    private String syncStatus;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.syncStatus == null) {
            this.syncStatus = "PENDING";
        }
        if (this.imapPort == 0) {
            this.imapPort = 993;
        }
        if (this.smtpPort == 0) {
            this.smtpPort = 587;
        }
    }
}
