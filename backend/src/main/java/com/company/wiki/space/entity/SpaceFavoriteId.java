package com.company.wiki.space.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public record SpaceFavoriteId(Long spaceId, Long userId) implements Serializable {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpaceFavoriteId that)) return false;
        return Objects.equals(spaceId, that.spaceId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spaceId, userId);
    }
}
