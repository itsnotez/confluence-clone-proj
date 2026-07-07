package com.company.wiki.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ContentDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateRequest {
        private Long parentId;
        @Builder.Default
        private String type = "PAGE";
        @NotBlank
        private String title;
        private String body;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateRequest {
        private String title;
        private String body;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PublishRequest {
        @NotBlank
        private String body;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserSummary {
        private Long id;
        private String name;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private Long spaceId;
        private Long parentId;
        private String type;
        private String title;
        private String status;
        private Long currentVersionId;
        private int position;
        private UserSummary createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String body;
        private String bodyPreview;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TreeNode {
        private Long id;
        private Long spaceId;
        private Long parentId;
        private String type;
        private String title;
        private String status;
        private Long currentVersionId;
        private int position;
        private UserSummary createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String bodyPreview;
        private List<TreeNode> children;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MoveRequest {
        private Long parentId; // null = root
        private int position;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VersionResponse {
        private Long id;
        private Long contentId;
        private int versionNo;
        private Long authorId;
        private String authorName;
        private LocalDateTime createdAt;
        private String body;
    }
}
