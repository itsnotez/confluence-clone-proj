package com.company.wiki.comment.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        private String body;
        private Long parentCommentId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateRequest {
        private String body;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommentNode {
        private Long id;
        private String body;
        private Long authorId;
        private Long parentCommentId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        @Builder.Default
        private List<CommentNode> children = new ArrayList<>();
    }
}
