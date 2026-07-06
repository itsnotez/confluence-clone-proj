package com.company.wiki.label.repository;

import com.company.wiki.label.entity.ContentLabel;
import com.company.wiki.label.entity.ContentLabelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ContentLabelRepository extends JpaRepository<ContentLabel, ContentLabelId> {

    List<ContentLabel> findByIdContentId(Long contentId);

    @Modifying
    void deleteByIdContentIdAndIdLabelId(Long contentId, Long labelId);
}
