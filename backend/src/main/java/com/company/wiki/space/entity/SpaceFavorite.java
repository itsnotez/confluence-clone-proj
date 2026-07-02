package com.company.wiki.space.entity;

import com.company.wiki.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "space_favorites")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SpaceFavorite {

    @EmbeddedId
    private SpaceFavoriteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("spaceId")
    @JoinColumn(name = "space_id")
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
