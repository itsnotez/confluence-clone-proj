package com.company.wiki.content.service;

import com.company.wiki.auditlog.service.AuditLogService;
import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.content.dto.ContentDto;
import com.company.wiki.content.entity.Content;
import com.company.wiki.content.entity.ContentVersion;
import com.company.wiki.content.repository.ContentRepository;
import com.company.wiki.content.repository.ContentSearchBodyRepository;
import com.company.wiki.content.repository.ContentVersionRepository;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.space.entity.Space;
import com.company.wiki.space.repository.SpaceRepository;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.GroupMemberRepository;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentVersionRepository contentVersionRepository;
    private final ContentSearchBodyRepository contentSearchBodyRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final GroupMemberRepository groupMemberRepository;
    private final AuditLogService auditLogService;

    private List<Long> getUserGroupIds(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(gm -> gm.getId().getGroupId())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // 트리 조회
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ContentDto.TreeNode> getContentTree(String spaceKey, Long currentUserId, String userRole) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        if (!permissionService.canRead(space.getId(), currentUserId, userRole, getUserGroupIds(currentUserId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        List<Content> all = contentRepository.findBySpaceIdAndDeletedAtIsNull(space.getId());

        // Map<parentId, List<Content>> — null parentId는 null 키로 처리
        Map<Long, List<Content>> byParent = new HashMap<>();
        for (Content c : all) {
            Long key = c.getParentId(); // null이면 root
            byParent.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }

        List<Content> roots = byParent.getOrDefault(null, List.of());
        return roots.stream()
                .map(c -> buildTreeNode(c, byParent))
                .collect(Collectors.toList());
    }

    private ContentDto.TreeNode buildTreeNode(Content c, Map<Long, List<Content>> byParent) {
        List<ContentDto.TreeNode> children = byParent.getOrDefault(c.getId(), List.of())
                .stream()
                .map(child -> buildTreeNode(child, byParent))
                .collect(Collectors.toList());

        return ContentDto.TreeNode.builder()
                .id(c.getId())
                .spaceId(c.getSpaceId())
                .parentId(c.getParentId())
                .type(c.getType())
                .title(c.getTitle())
                .status(c.getStatus())
                .currentVersionId(c.getCurrentVersionId())
                .position(c.getPosition())
                .createdBy(toUserSummary(c.getCreatedBy()))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .children(children)
                .build();
    }

    // -------------------------------------------------------
    // 단건 조회
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public ContentDto.Response getContent(Long contentId, Long currentUserId, String userRole) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!permissionService.canRead(content.getSpaceId(), currentUserId, userRole, getUserGroupIds(currentUserId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        String body = null;
        if (content.getCurrentVersionId() != null) {
            body = contentVersionRepository.findById(content.getCurrentVersionId())
                    .map(ContentVersion::getBody).orElse(null);
        }

        return toResponse(content, body);
    }

    // -------------------------------------------------------
    // 생성
    // -------------------------------------------------------
    public ContentDto.Response createContent(String spaceKey, ContentDto.CreateRequest req,
                                             Long createdById, String userRole) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        if (!permissionService.canWrite(space.getId(), createdById, userRole, getUserGroupIds(createdById))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Content content = Content.builder()
                .spaceId(space.getId())
                .parentId(req.getParentId())
                .type(req.getType() != null ? req.getType() : "PAGE")
                .title(req.getTitle())
                .status("DRAFT")
                .position(0)
                .createdBy(creator)
                .build();

        content = contentRepository.save(content);

        String body = null;
        if (req.getBody() != null && !req.getBody().isBlank()) {
            ContentVersion version = ContentVersion.builder()
                    .contentId(content.getId())
                    .versionNo(1)
                    .body(req.getBody())
                    .author(creator)
                    .build();
            version = contentVersionRepository.save(version);
            content.setCurrentVersionId(version.getId());
            content = contentRepository.save(content);
            body = version.getBody();
            contentSearchBodyRepository.upsertSearchBody(content.getId(), body);
        }

        return toResponse(content, body);
    }

    // -------------------------------------------------------
    // 수정
    // -------------------------------------------------------
    public ContentDto.Response updateContent(Long contentId, ContentDto.UpdateRequest req,
                                             Long userId, String userRole) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!permissionService.canWrite(content.getSpaceId(), userId, userRole, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            content.setTitle(req.getTitle());
        }

        String body = null;
        if (req.getBody() != null) {
            if (content.getCurrentVersionId() != null) {
                // Draft 덮어쓰기
                contentVersionRepository.findById(content.getCurrentVersionId()).ifPresent(v -> {
                    v.setBody(req.getBody());
                    contentVersionRepository.save(v);
                });
                body = req.getBody();
            } else {
                // 버전이 없으면 새로 생성
                User author = userRepository.findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                ContentVersion version = ContentVersion.builder()
                        .contentId(content.getId())
                        .versionNo(1)
                        .body(req.getBody())
                        .author(author)
                        .build();
                version = contentVersionRepository.save(version);
                content.setCurrentVersionId(version.getId());
                body = version.getBody();
            }
            contentSearchBodyRepository.upsertSearchBody(content.getId(), body);
        }

        content = contentRepository.save(content);
        return toResponse(content, body);
    }

    // -------------------------------------------------------
    // 게시
    // -------------------------------------------------------
    public ContentDto.Response publishContent(Long contentId, ContentDto.PublishRequest req,
                                              Long userId, String userRole) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!permissionService.canWrite(content.getSpaceId(), userId, userRole, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int nextVersionNo = contentVersionRepository.countByContentId(contentId) + 1;

        ContentVersion version = ContentVersion.builder()
                .contentId(contentId)
                .versionNo(nextVersionNo)
                .body(req.getBody())
                .author(author)
                .build();
        version = contentVersionRepository.save(version);

        content.setCurrentVersionId(version.getId());
        content.setStatus("PUBLISHED");
        content = contentRepository.save(content);
        contentSearchBodyRepository.upsertSearchBody(contentId, req.getBody());

        return toResponse(content, version.getBody());
    }

    // -------------------------------------------------------
    // 삭제
    // -------------------------------------------------------
    public void deleteContent(Long contentId, Long userId, String userRole) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!permissionService.canWrite(content.getSpaceId(), userId, userRole, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        content.setDeletedAt(LocalDateTime.now());
        content.setStatus("ARCHIVED");
        contentRepository.save(content);

        try {
            auditLogService.record(userId, "CONTENT_DELETE", "CONTENT", contentId,
                    Map.of("title", content.getTitle() != null ? content.getTitle() : "",
                           "spaceId", content.getSpaceId()),
                    false);
        } catch (Exception e) {
            log.warn("audit failed: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 버전 목록
    // -------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ContentDto.VersionResponse> getVersions(Long contentId, Long userId, String userRole) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!permissionService.canRead(content.getSpaceId(), userId, userRole, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        return contentVersionRepository.findByContentIdOrderByVersionNoDesc(contentId)
                .stream()
                .map(v -> ContentDto.VersionResponse.builder()
                        .id(v.getId())
                        .contentId(v.getContentId())
                        .versionNo(v.getVersionNo())
                        .authorId(v.getAuthor() != null ? v.getAuthor().getId() : null)
                        .authorName(v.getAuthor() != null ? v.getAuthor().getName() : null)
                        .createdAt(v.getCreatedAt())
                        .body(v.getBody())
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // 내부 변환 헬퍼
    // -------------------------------------------------------
    private ContentDto.Response toResponse(Content c, String body) {
        String preview = null;
        if (body != null) {
            preview = body.length() > 200 ? body.substring(0, 200) : body;
        }
        return ContentDto.Response.builder()
                .id(c.getId())
                .spaceId(c.getSpaceId())
                .parentId(c.getParentId())
                .type(c.getType())
                .title(c.getTitle())
                .status(c.getStatus())
                .currentVersionId(c.getCurrentVersionId())
                .position(c.getPosition())
                .createdBy(toUserSummary(c.getCreatedBy()))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .bodyPreview(preview)
                .build();
    }

    private ContentDto.UserSummary toUserSummary(User user) {
        if (user == null) return null;
        return ContentDto.UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
