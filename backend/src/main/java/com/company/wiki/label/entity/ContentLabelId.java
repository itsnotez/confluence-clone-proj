package com.company.wiki.label.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public record ContentLabelId(
    @Column(name = "content_id") Long contentId,
    @Column(name = "label_id")   Long labelId
) implements Serializable {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentLabelId that)) return false;
        return Objects.equals(contentId, that.contentId)
            && Objects.equals(labelId, that.labelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentId, labelId);
    }
}
