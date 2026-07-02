package com.company.wiki.permission.repository;

import com.company.wiki.permission.entity.SpacePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpacePermissionRepository extends JpaRepository<SpacePermission, Long> {

    List<SpacePermission> findBySpaceId(Long spaceId);

    Optional<SpacePermission> findBySpaceIdAndSubjectTypeAndSubjectId(
            Long spaceId, String subjectType, Long subjectId);

    // ALL 타입 조회용 (subject_id가 NULL인 레코드)
    Optional<SpacePermission> findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(
            Long spaceId, String subjectType);

    void deleteBySpaceIdAndSubjectTypeAndSubjectId(
            Long spaceId, String subjectType, Long subjectId);

    void deleteBySpaceIdAndSubjectTypeAndSubjectIdIsNull(
            Long spaceId, String subjectType);
}
