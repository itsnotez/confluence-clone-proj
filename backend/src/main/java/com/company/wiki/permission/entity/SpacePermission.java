package com.company.wiki.permission.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "space_permissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"space_id", "subject_type", "subject_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SpacePermission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SpaceRepository 의존성 제거를 위해 @ManyToOne 대신 Long ID 사용
    @Column(name = "space_id", nullable = false)
    private Long spaceId;

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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
