package com.company.wiki.label.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_labels")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ContentLabel {

    @EmbeddedId
    private ContentLabelId id;
    // 순수 junction table — 다른 컬럼 없음
}
