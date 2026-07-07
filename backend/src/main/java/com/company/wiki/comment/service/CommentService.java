package com.company.wiki.comment.service;

import com.company.wiki.comment.dto.CommentDto;
import com.company.wiki.comment.entity.Comment;
import com.company.wiki.comment.repository.CommentRepository;
import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.content.entity.Content;
import com.company.wiki.content.repository.ContentRepository;
import com.company.wiki.notification.service.NotificationService;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.user.repository.GroupMemberRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final ContentRepository contentRepository;
    private final PermissionService permissionService;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;

    private List<Long> getUserGroupIds(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(gm -> gm.getId().getGroupId())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDto.CommentNode> getComments(Long contentId, Long userId, String role) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canRead(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        List<Comment> all = commentRepository.findByContentIdAndDeletedAtIsNull(contentId);
        Map<Long, List<Comment>> byParent = new HashMap<>();
        for (Comment c : all) {
            byParent.computeIfAbsent(c.getParentCommentId(), k -> new ArrayList<>()).add(c);
        }
        return byParent.getOrDefault(null, List.of()).stream()
                .map(c -> buildCommentNode(c, byParent))
                .collect(Collectors.toList());
    }

    private CommentDto.CommentNode buildCommentNode(Comment c, Map<Long, List<Comment>> byParent) {
        List<CommentDto.CommentNode> children = byParent.getOrDefault(c.getId(), List.of()).stream()
                .map(child -> buildCommentNode(child, byParent))
                .collect(Collectors.toList());

        return CommentDto.CommentNode.builder()
                .id(c.getId())
                .body(c.getBody())
                .authorId(c.getAuthorId())
                .parentCommentId(c.getParentCommentId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .children(children)
                .build();
    }

    public CommentDto.CommentNode createComment(Long contentId, CommentDto.CreateRequest req,
                                               Long userId, String role) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (!permissionService.canWrite(content.getSpaceId(), userId, role, getUserGroupIds(userId))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        Comment comment = Comment.builder()
                .contentId(contentId)
                .parentCommentId(req.getParentCommentId())
                .body(req.getBody())
                .authorId(userId)
                .build();
        comment = commentRepository.save(comment);

        try {
            Long contentAuthorId = content.getCreatedBy().getId();
            if (!contentAuthorId.equals(userId)) { // 자기 글에 자기 댓글은 알림 제외
                notificationService.create(
                        contentAuthorId,
                        "COMMENT",
                        "새 댓글이 달렸습니다",
                        content.getTitle() + "에 댓글이 달렸습니다.",
                        "/spaces/" + content.getSpaceId() + "/contents/" + contentId
                );
            }
        } catch (Exception e) {
            log.warn("알림 생성 실패 (비중단): {}", e.getMessage());
        }

        return buildCommentNode(comment, new HashMap<>());
    }

    public CommentDto.CommentNode updateComment(Long commentId, CommentDto.UpdateRequest req,
                                               Long userId, String role) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        checkAuthorOrAdmin(comment, userId, role);
        comment.setBody(req.getBody());
        comment = commentRepository.save(comment);
        return buildCommentNode(comment, new HashMap<>());
    }

    public void deleteComment(Long commentId, Long userId, String role) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        checkAuthorOrAdmin(comment, userId, role);
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    private void checkAuthorOrAdmin(Comment comment, Long userId, String role) {
        if (!comment.getAuthorId().equals(userId) && !"SITE_ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }
}
