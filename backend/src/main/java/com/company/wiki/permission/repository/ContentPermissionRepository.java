package com.company.wiki.permission.repository;

import com.company.wiki.permission.entity.ContentPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentPermissionRepository extends JpaRepository<ContentPermission, Long> {

    List<ContentPermission> findByContentId(Long contentId);

    Optional<ContentPermission> findByContentIdAndSubjectTypeAndSubjectId(
            Long contentId, String subjectType, Long subjectId);

    Optional<ContentPermission> findByContentIdAndSubjectTypeAndSubjectIdIsNull(
            Long contentId, String subjectType);
}
