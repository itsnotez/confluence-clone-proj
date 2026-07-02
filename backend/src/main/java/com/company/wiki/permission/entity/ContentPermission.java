package com.company.wiki.permission.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "content_permissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"content_id", "subject_type", "subject_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContentPermission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    // "USER" | "GROUP" | "ALL"
    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;

    // USER/GROUP일 때 사용자/그룹 ID, ALL일 때 null
    @Column(name = "subject_id")
    private Long subjectId;

    // "SPACE_ADMIN" | "WRITE" | "READ" | "NONE"
    @Column(name = "permission_level", nullable = false, length = 30)
    private String permissionLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
