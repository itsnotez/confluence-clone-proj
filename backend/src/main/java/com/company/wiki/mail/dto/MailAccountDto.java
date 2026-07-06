package com.company.wiki.mail.dto;

import com.company.wiki.mail.entity.MailAccount;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class MailAccountDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank
        private String emailAddress;

        @NotBlank
        private String password;

        @NotBlank
        private String imapHost;

        @Builder.Default
        private int imapPort = 993;

        @Builder.Default
        private boolean imapSsl = true;

        private String smtpHost;

        @Builder.Default
        private int smtpPort = 587;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long id;
        private Long spaceId;
        private String emailAddress;
        private String imapHost;
        private int imapPort;
        private boolean imapSsl;
        private String syncStatus;
        private LocalDateTime lastSyncedAt;
        private LocalDateTime createdAt;

        public static Response from(MailAccount account) {
            return Response.builder()
                    .id(account.getId())
                    .spaceId(account.getSpaceId())
                    .emailAddress(account.getEmailAddress())
                    .imapHost(account.getImapHost())
                    .imapPort(account.getImapPort())
                    .imapSsl(account.isImapSsl())
                    .syncStatus(account.getSyncStatus())
                    .lastSyncedAt(account.getLastSyncedAt())
                    .createdAt(account.getCreatedAt())
                    .build();
        }
    }
}
