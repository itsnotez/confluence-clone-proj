package com.company.wiki.space.dto;

import com.company.wiki.space.entity.Space;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SpaceDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "Space Key는 필수입니다.")
        @Pattern(regexp = "[A-Z0-9_]{2,50}", message = "Space Key는 대문자, 숫자, 언더스코어 2~50자여야 합니다.")
        private String spaceKey;

        @NotBlank(message = "Space 이름은 필수입니다.")
        private String name;

        private String description;

        @Builder.Default
        private String type = "PRIVATE";

        private String iconEmoji;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String name;
        private String description;
        private String type;
        private String iconEmoji;
    }

    @Getter
    @Builder
    public static class UserSummary {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String spaceKey;
        private String name;
        private String description;
        private String type;
        private String status;
        private String iconEmoji;
        private UserSummary createdBy;
        private LocalDateTime createdAt;
        private boolean favorited;

        public static Response from(Space space, boolean favorited) {
            UserSummary userSummary = null;
            if (space.getCreatedBy() != null) {
                userSummary = UserSummary.builder()
                        .id(space.getCreatedBy().getId())
                        .name(space.getCreatedBy().getName())
                        .build();
            }

            return Response.builder()
                    .id(space.getId())
                    .spaceKey(space.getSpaceKey())
                    .name(space.getName())
                    .description(space.getDescription())
                    .type(space.getType())
                    .status(space.getStatus())
                    .iconEmoji(space.getIconEmoji())
                    .createdBy(userSummary)
                    .createdAt(space.getCreatedAt())
                    .favorited(favorited)
                    .build();
        }
    }
}
