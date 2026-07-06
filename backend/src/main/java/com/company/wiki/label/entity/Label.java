package com.company.wiki.label.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "labels")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id")
    private Long spaceId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String color;
    // labels 테이블에 deleted_at 없음 (V3 스키마) — 물리 삭제 허용
}
