package com.company.wiki.permission.dto;

import com.company.wiki.permission.entity.SpacePermission;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

public class PermissionDto {

    /**
     * 권한 부여 요청 DTO
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class GrantRequest {

        @NotBlank(message = "subjectType은 필수입니다.")
        private String subjectType;  // "USER" | "GROUP" | "ALL"

        private Long subjectId;      // USER/GROUP인 경우 ID, ALL인 경우 null

        @NotBlank(message = "permissionLevel은 필수입니다.")
        private String permissionLevel;  // "SPACE_ADMIN" | "WRITE" | "READ" | "NONE"
    }

    /**
     * 권한 삭제 요청 DTO (쿼리 파라미터)
     */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class RevokeRequest {
        private String subjectType;
        private Long subjectId;
    }

    /**
     * 권한 응답 DTO
     */
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long spaceId;
        private String subjectType;
        private Long subjectId;
        private String permissionLevel;
        private LocalDateTime createdAt;

        public static Response from(SpacePermission sp) {
            return Response.builder()
                    .id(sp.getId())
                    .spaceId(sp.getSpaceId())
                    .subjectType(sp.getSubjectType())
                    .subjectId(sp.getSubjectId())
                    .permissionLevel(sp.getPermissionLevel())
                    .createdAt(sp.getCreatedAt())
                    .build();
        }
    }
}
