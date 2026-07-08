package com.company.wiki.mail.service;

import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.content.entity.Content;
import com.company.wiki.content.entity.ContentVersion;
import com.company.wiki.content.repository.ContentRepository;
import com.company.wiki.content.repository.ContentVersionRepository;
import com.company.wiki.mail.dto.MailAttachmentDto;
import com.company.wiki.mail.dto.MailMessageDto;
import com.company.wiki.mail.entity.MailAccount;
import com.company.wiki.mail.entity.MailMessage;
import com.company.wiki.mail.entity.MailMessageAttachment;
import com.company.wiki.mail.repository.MailAccountRepository;
import com.company.wiki.mail.repository.MailMessageAttachmentRepository;
import com.company.wiki.mail.repository.MailMessageRepository;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.space.entity.Space;
import com.company.wiki.space.repository.SpaceRepository;
import com.company.wiki.user.entity.User;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MailMessageService {

    private final MailMessageRepository mailMessageRepository;
    private final MailAccountRepository mailAccountRepository;
    private final MailMessageAttachmentRepository mailMessageAttachmentRepository;
    private final ContentRepository contentRepository;
    private final ContentVersionRepository contentVersionRepository;
    private final SpaceRepository spaceRepository;
    private final PermissionService permissionService;
    private final UserRepository userRepository;

    /**
     * 특정 메일 계정의 메시지 목록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<MailMessageDto.Response> findByAccount(
            String spaceKey, Long accountId, Long userId, String userRole, List<Long> groupIds) {

        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        String permission = permissionService.resolveSpacePermission(space.getId(), userId, groupIds);
        if ("NONE".equals(permission) && !"SITE_ADMIN".equals(userRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        mailAccountRepository.findByIdAndSpaceId(accountId, space.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        return mailMessageRepository.findByMailAccountIdOrderByReceivedAtDesc(accountId)
                .stream()
                .map(MailMessageDto.Response::from)
                .toList();
    }

    /**
     * 메일 메시지를 Wiki 페이지로 변환
     */
    public MailMessageDto.ConvertResponse convertToPage(
            String spaceKey, Long accountId, Long msgId, Long userId, String userRole, List<Long> groupIds) {

        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        boolean isAdmin = permissionService.isSpaceAdmin(space.getId(), userId, userRole, groupIds);
        if (!isAdmin) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        mailAccountRepository.findByIdAndSpaceId(accountId, space.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        MailMessage msg = mailMessageRepository.findByIdAndMailAccountId(msgId, accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_MESSAGE_NOT_FOUND));

        if (msg.getLinkedContentId() != null) {
            throw new BusinessException(ErrorCode.ALREADY_CONVERTED);
        }

        // 본문 JSON 생성
        String bodyText = msg.getBodyText();
        String bodyJson;
        if (bodyText == null || bodyText.isBlank()) {
            bodyJson = "{\"type\":\"doc\",\"content\":[]}";
        } else {
            String escaped = bodyText
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
            bodyJson = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
                    + escaped + "\"}]}]}";
        }

        // 작성자 조회
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // Content 생성
        String title = msg.getSubject() != null ? msg.getSubject() : "(제목 없음)";
        Content content = Content.builder()
                .spaceId(space.getId())
                .parentId(null)
                .title(title)
                .type("PAGE")
                .status("PUBLISHED")
                .createdBy(author)
                .position(0)
                .build();
        content = contentRepository.save(content);

        // ContentVersion 생성
        ContentVersion version = ContentVersion.builder()
                .contentId(content.getId())
                .versionNo(1)
                .body(bodyJson)
                .author(author)
                .build();
        version = contentVersionRepository.save(version);

        // Content currentVersionId 업데이트
        content.setCurrentVersionId(version.getId());
        contentRepository.save(content);

        // MailMessage 상태 업데이트
        msg.setLinkedContentId(content.getId());
        msg.setStatus("CONVERTED");
        mailMessageRepository.save(msg);

        log.info("메일 메시지 [{}] → 페이지 [{}] 변환 완료", msgId, content.getId());

        return new MailMessageDto.ConvertResponse(content.getId(), content.getTitle(), "메일이 페이지로 변환되었습니다");
    }

    /**
     * 첨부파일 메타 목록 조회 (파일 데이터 제외)
     */
    @Transactional(readOnly = true)
    public List<MailAttachmentDto.Meta> getAttachmentMeta(
            String spaceKey, Long accountId, Long msgId, Long userId, String userRole, List<Long> groupIds) {

        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        String permission = permissionService.resolveSpacePermission(space.getId(), userId, groupIds);
        if ("NONE".equals(permission) && !"SITE_ADMIN".equals(userRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        mailAccountRepository.findByIdAndSpaceId(accountId, space.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        mailMessageRepository.findByIdAndMailAccountId(msgId, accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_MESSAGE_NOT_FOUND));

        return mailMessageAttachmentRepository.findMetaByMailMessageId(msgId)
                .stream()
                .map(row -> MailAttachmentDto.Meta.builder()
                        .id((Long) row[0])
                        .fileName((String) row[1])
                        .contentType((String) row[2])
                        .fileSize((Long) row[3])
                        .build())
                .toList();
    }

    /**
     * 첨부파일 다운로드용 엔티티 조회
     */
    @Transactional(readOnly = true)
    public MailMessageAttachment downloadAttachment(
            String spaceKey, Long accountId, Long msgId, Long attachId, Long userId, String userRole, List<Long> groupIds) {

        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        String permission = permissionService.resolveSpacePermission(space.getId(), userId, groupIds);
        if ("NONE".equals(permission) && !"SITE_ADMIN".equals(userRole)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        mailAccountRepository.findByIdAndSpaceId(accountId, space.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        mailMessageRepository.findByIdAndMailAccountId(msgId, accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_MESSAGE_NOT_FOUND));

        return mailMessageAttachmentRepository.findById(attachId)
                .filter(a -> a.getMailMessageId().equals(msgId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
    }
}
