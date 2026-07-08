package com.company.wiki.search.dto;

import lombok.*;

import java.time.LocalDateTime;

public class SearchDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private Long spaceId;
        private String spaceKey;
        private String status;
        private LocalDateTime updatedAt;
        private Double rank;
    }
}
