package com.company.wiki.space.service;

import com.company.wiki.auditlog.service.AuditLogService;
import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.space.dto.SpaceDto;
import com.company.wiki.space.entity.Space;
import com.company.wiki.space.entity.SpaceFavorite;
import com.company.wiki.space.entity.SpaceFavoriteId;
import com.company.wiki.permission.repository.SpacePermissionRepository;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.space.repository.SpaceFavoriteRepository;
import com.company.wiki.space.repository.SpaceRepository;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceFavoriteRepository spaceFavoriteRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SpacePermissionRepository spacePermissionRepository;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<SpaceDto.Response> findAll(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        boolean isSiteAdmin = "SITE_ADMIN".equals(currentUser.getRole());

        return spaceRepository.findByStatusAndDeletedAtIsNull("ACTIVE")
                .stream()
                .filter(space -> {
                    if ("PUBLIC".equals(space.getType())) return true;
                    if (isSiteAdmin) return true;
                    // PRIVATE: 해당 유저에게 명시적 권한이 있는지 확인
                    return spacePermissionRepository
                            .findBySpaceIdAndSubjectTypeAndSubjectId(space.getId(), "USER", currentUserId)
                            .isPresent()
                            || spacePermissionRepository
                            .findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(space.getId(), "ALL")
                            .isPresent();
                })
                .map(space -> {
                    boolean favorited = spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(
                            space.getId(), currentUserId);
                    return SpaceDto.Response.from(space, favorited);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpaceDto.Response findByKey(String spaceKey, Long currentUserId) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
        boolean favorited = spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(
                space.getId(), currentUserId);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String myPermission = "SITE_ADMIN".equals(currentUser.getRole())
                ? "SPACE_ADMIN"
                : permissionService.resolveSpacePermission(space.getId(), currentUserId, List.of());
        return SpaceDto.Response.from(space, favorited, myPermission);
    }

    public SpaceDto.Response create(SpaceDto.CreateRequest req, Long createdById) {
        if (spaceRepository.existsBySpaceKey(req.getSpaceKey())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SPACE_KEY);
        }

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Space space = Space.builder()
                .spaceKey(req.getSpaceKey())
                .name(req.getName())
                .description(req.getDescription())
                .type(req.getType() != null ? req.getType() : "PRIVATE")
                .iconEmoji(req.getIconEmoji())
                .createdBy(creator)
                .build();

        Space saved = spaceRepository.save(space);
        return SpaceDto.Response.from(saved, false);
    }

    public SpaceDto.Response update(String spaceKey, SpaceDto.UpdateRequest req, Long currentUserId) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        if ("ARCHIVED".equals(space.getStatus())) {
            throw new BusinessException(ErrorCode.ARCHIVED_SPACE);
        }

        if (req.getName() != null) {
            space.setName(req.getName());
        }
        if (req.getDescription() != null) {
            space.setDescription(req.getDescription());
        }
        if (req.getType() != null) {
            space.setType(req.getType());
        }
        if (req.getIconEmoji() != null) {
            space.setIconEmoji(req.getIconEmoji());
        }

        Space saved = spaceRepository.save(space);
        boolean favorited = spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(
                saved.getId(), currentUserId);
        return SpaceDto.Response.from(saved, favorited);
    }

    public void delete(String spaceKey) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        space.setDeletedAt(LocalDateTime.now());
        space.setStatus("DELETED");
        spaceRepository.save(space);

        try {
            Long actorId = Long.parseLong(
                    SecurityContextHolder.getContext().getAuthentication().getName());
            auditLogService.record(actorId, "SPACE_DELETE", "SPACE", space.getId(),
                    Map.of("spaceKey", space.getSpaceKey(), "name", space.getName()),
                    false);
        } catch (Exception e) {
            log.warn("audit failed: {}", e.getMessage());
        }
    }

    public void toggleFavorite(String spaceKey, Long userId) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        Long spaceId = space.getId();

        if (spaceFavoriteRepository.existsByIdSpaceIdAndIdUserId(spaceId, userId)) {
            spaceFavoriteRepository.deleteBySpaceIdAndUserId(spaceId, userId);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            SpaceFavorite favorite = SpaceFavorite.builder()
                    .id(new SpaceFavoriteId(spaceId, userId))
                    .space(space)
                    .user(user)
                    .build();
            spaceFavoriteRepository.save(favorite);
        }
    }
}
