package com.company.wiki.attachment.dto;

import lombok.*;
import java.time.LocalDateTime;

public class AttachmentDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long contentId;
        private String fileName;
        private String mimeType;
        private Long sizeBytes;
        private Integer version;
        private Long uploadedBy;
        private LocalDateTime createdAt;
    }
}
