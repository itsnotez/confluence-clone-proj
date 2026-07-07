package com.company.wiki.permission.service;

import com.company.wiki.auditlog.service.AuditLogService;
import com.company.wiki.permission.entity.ContentPermission;
import com.company.wiki.permission.entity.SpacePermission;
import com.company.wiki.permission.repository.ContentPermissionRepository;
import com.company.wiki.permission.repository.SpacePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RBAC 권한 판단 서비스
 *
 * 권한 레벨 우선순위 (높을수록 강함): SPACE_ADMIN > WRITE > READ > NONE
 * 판단 우선순위: 개인(USER) > 그룹(GROUP) > 전체(ALL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final SpacePermissionRepository spacePermissionRepository;
    private final ContentPermissionRepository contentPermissionRepository;
    private final AuditLogService auditLogService;

    // 권한 레벨 순위 (낮은 숫자 = 낮은 권한)
    private static final List<String> PERMISSION_ORDER = List.of("NONE", "READ", "WRITE", "SPACE_ADMIN");

    private int permissionRank(String level) {
        int idx = PERMISSION_ORDER.indexOf(level);
        return idx < 0 ? 0 : idx;
    }

    /**
     * Space 권한 조회 — 개인 > 그룹 > 전체 우선순위
     *
     * @param spaceId      Space ID
     * @param userId       사용자 ID
     * @param userGroupIds 사용자가 속한 그룹 ID 목록
     * @return "SPACE_ADMIN" | "WRITE" | "READ" | "NONE"
     */
    public String resolveSpacePermission(Long spaceId, Long userId, List<Long> userGroupIds) {
        // 1. 개인(USER) 권한 조회 — 있으면 최우선 반환
        if (userId != null) {
            Optional<SpacePermission> personal = spacePermissionRepository
                    .findBySpaceIdAndSubjectTypeAndSubjectId(spaceId, "USER", userId);
            if (personal.isPresent()) {
                return personal.get().getPermissionLevel();
            }
        }

        // 2. 그룹(GROUP) 권한 조회 — 복수 그룹 중 가장 높은 권한 반환
        if (userGroupIds != null && !userGroupIds.isEmpty()) {
            String bestGroupPermission = userGroupIds.stream()
                    .map(groupId -> spacePermissionRepository
                            .findBySpaceIdAndSubjectTypeAndSubjectId(spaceId, "GROUP", groupId))
                    .filter(Optional::isPresent)
                    .map(opt -> opt.get().getPermissionLevel())
                    .max(Comparator.comparingInt(this::permissionRank))
                    .orElse(null);

            if (bestGroupPermission != null) {
                return bestGroupPermission;
            }
        }

        // 3. 전체(ALL) 권한 조회
        Optional<SpacePermission> allPerm = spacePermissionRepository
                .findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(spaceId, "ALL");
        if (allPerm.isPresent()) {
            return allPerm.get().getPermissionLevel();
        }

        // 4. 권한 없음
        return "NONE";
    }

    /**
     * Space 읽기 권한 판단
     * - SITE_ADMIN은 항상 true
     * - READ / WRITE / SPACE_ADMIN 이상이면 true
     */
    public boolean canRead(Long spaceId, Long userId, String userRole, List<Long> userGroupIds) {
        if ("SITE_ADMIN".equals(userRole)) return true;
        String level = resolveSpacePermission(spaceId, userId, userGroupIds);
        return permissionRank(level) >= permissionRank("READ");
    }

    /**
     * Space 쓰기 권한 판단
     * - SITE_ADMIN은 항상 true
     * - WRITE / SPACE_ADMIN 이상이면 true
     */
    public boolean canWrite(Long spaceId, Long userId, String userRole, List<Long> userGroupIds) {
        if ("SITE_ADMIN".equals(userRole)) return true;
        String level = resolveSpacePermission(spaceId, userId, userGroupIds);
        return permissionRank(level) >= permissionRank("WRITE");
    }

    /**
     * Space 관리자 권한 판단
     * - SITE_ADMIN은 항상 true
     * - SPACE_ADMIN이면 true
     */
    public boolean isSpaceAdmin(Long spaceId, Long userId, String userRole, List<Long> userGroupIds) {
        if ("SITE_ADMIN".equals(userRole)) return true;
        String level = resolveSpacePermission(spaceId, userId, userGroupIds);
        return "SPACE_ADMIN".equals(level);
    }

    /**
     * 콘텐츠 권한 조회 — 콘텐츠 권한이 없으면 Space 권한 상속
     */
    public String resolveContentPermission(Long contentId, Long spaceId, Long userId,
                                           String userRole, List<Long> userGroupIds) {
        if ("SITE_ADMIN".equals(userRole)) return "SPACE_ADMIN";

        // 1. 개인 콘텐츠 권한 조회
        if (userId != null) {
            Optional<ContentPermission> personal = contentPermissionRepository
                    .findByContentIdAndSubjectTypeAndSubjectId(contentId, "USER", userId);
            if (personal.isPresent()) {
                return personal.get().getPermissionLevel();
            }
        }

        // 2. 그룹 콘텐츠 권한 조회
        if (userGroupIds != null && !userGroupIds.isEmpty()) {
            String bestGroupPermission = userGroupIds.stream()
                    .map(groupId -> contentPermissionRepository
                            .findByContentIdAndSubjectTypeAndSubjectId(contentId, "GROUP", groupId))
                    .filter(Optional::isPresent)
                    .map(opt -> opt.get().getPermissionLevel())
                    .max(Comparator.comparingInt(this::permissionRank))
                    .orElse(null);

            if (bestGroupPermission != null) {
                return bestGroupPermission;
            }
        }

        // 3. 전체 콘텐츠 권한 조회
        Optional<ContentPermission> allPerm = contentPermissionRepository
                .findByContentIdAndSubjectTypeAndSubjectIdIsNull(contentId, "ALL");
        if (allPerm.isPresent()) {
            return allPerm.get().getPermissionLevel();
        }

        // 4. 콘텐츠 권한 없음 → Space 권한 상속
        return resolveSpacePermission(spaceId, userId, userGroupIds);
    }

    /**
     * Space 권한 부여 — 기존 권한이 있으면 업데이트, 없으면 신규 생성
     */
    @Transactional
    public SpacePermission grantSpacePermission(Long spaceId, String subjectType,
                                                Long subjectId, String permissionLevel) {
        Optional<SpacePermission> existing;

        if ("ALL".equals(subjectType)) {
            existing = spacePermissionRepository
                    .findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(spaceId, subjectType);
        } else {
            existing = spacePermissionRepository
                    .findBySpaceIdAndSubjectTypeAndSubjectId(spaceId, subjectType, subjectId);
        }

        SpacePermission saved;
        if (existing.isPresent()) {
            SpacePermission sp = existing.get();
            sp.setPermissionLevel(permissionLevel);
            saved = spacePermissionRepository.save(sp);
        } else {
            SpacePermission sp = SpacePermission.builder()
                    .spaceId(spaceId)
                    .subjectType(subjectType)
                    .subjectId("ALL".equals(subjectType) ? null : subjectId)
                    .permissionLevel(permissionLevel)
                    .build();
            saved = spacePermissionRepository.save(sp);
        }

        try {
            Long actorId = Long.parseLong(
                    SecurityContextHolder.getContext().getAuthentication().getName());
            auditLogService.record(actorId, "PERMISSION_CHANGE", "PERMISSION", spaceId,
                    Map.of("subjectType", subjectType != null ? subjectType : "",
                           "subjectId", subjectId != null ? subjectId : 0L,
                           "permissionLevel", permissionLevel != null ? permissionLevel : ""),
                    false);
        } catch (Exception e) {
            log.warn("audit failed: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Space 권한 삭제
     */
    @Transactional
    public void revokeSpacePermission(Long spaceId, String subjectType, Long subjectId) {
        if ("ALL".equals(subjectType)) {
            spacePermissionRepository
                    .deleteBySpaceIdAndSubjectTypeAndSubjectIdIsNull(spaceId, subjectType);
        } else {
            spacePermissionRepository
                    .deleteBySpaceIdAndSubjectTypeAndSubjectId(spaceId, subjectType, subjectId);
        }
    }

    /**
     * Space 권한 목록 조회
     */
    public List<SpacePermission> findSpacePermissions(Long spaceId) {
        return spacePermissionRepository.findBySpaceId(spaceId);
    }
}
