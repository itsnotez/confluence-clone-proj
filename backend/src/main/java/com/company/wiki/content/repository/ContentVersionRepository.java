package com.company.wiki.content.repository;

import com.company.wiki.content.entity.ContentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentVersionRepository extends JpaRepository<ContentVersion, Long> {

    List<ContentVersion> findByContentIdOrderByVersionNoDesc(Long contentId);

    Optional<ContentVersion> findByContentIdAndVersionNo(Long contentId, int versionNo);

    int countByContentId(Long contentId);
}
