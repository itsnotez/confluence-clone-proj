package com.company.wiki.mail.dto;

import lombok.Builder;
import lombok.Getter;

public class MailAttachmentDto {

    @Getter
    @Builder
    public static class Meta {
        private Long id;
        private String fileName;
        private String contentType;
        private Long fileSize;
    }
}
