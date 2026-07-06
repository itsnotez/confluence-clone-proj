package com.company.wiki.label.repository;

import com.company.wiki.label.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {

    List<Label> findBySpaceId(Long spaceId);
}
