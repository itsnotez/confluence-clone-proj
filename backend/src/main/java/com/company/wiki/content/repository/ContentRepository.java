package com.company.wiki.content.repository;

import com.company.wiki.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findBySpaceIdAndParentIdIsNullAndDeletedAtIsNull(Long spaceId);

    List<Content> findBySpaceIdAndParentIdAndDeletedAtIsNull(Long spaceId, Long parentId);

    List<Content> findBySpaceIdAndDeletedAtIsNull(Long spaceId);

    Optional<Content> findByIdAndDeletedAtIsNull(Long id);
}
