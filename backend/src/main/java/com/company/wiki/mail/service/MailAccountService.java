package com.company.wiki.mail.service;

import com.company.wiki.auditlog.service.AuditLogService;
import com.company.wiki.common.exception.BusinessException;
import com.company.wiki.common.exception.ErrorCode;
import com.company.wiki.common.util.AesEncryptUtil;
import com.company.wiki.mail.dto.MailAccountDto;
import com.company.wiki.mail.entity.MailAccount;
import com.company.wiki.mail.repository.MailAccountRepository;
import com.company.wiki.permission.service.PermissionService;
import com.company.wiki.space.entity.Space;
import com.company.wiki.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MailAccountService {

    private final SpaceRepository spaceRepository;
    private final MailAccountRepository mailAccountRepository;
    private final AesEncryptUtil aesEncryptUtil;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<MailAccountDto.Response> findBySpace(String spaceKey) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        return mailAccountRepository.findBySpaceId(space.getId())
                .stream()
                .map(MailAccountDto.Response::from)
                .collect(Collectors.toList());
    }

    public MailAccountDto.Response create(String spaceKey,
                                          MailAccountDto.CreateRequest req,
                                          Long userId,
                                          String userRole,
                                          List<Long> groupIds) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        if (!permissionService.isSpaceAdmin(space.getId(), userId, userRole, groupIds)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String encryptedCredential = aesEncryptUtil.encrypt(req.getPassword());

        MailAccount account = MailAccount.builder()
                .spaceId(space.getId())
                .emailAddress(req.getEmailAddress())
                .imapHost(req.getImapHost())
                .imapPort(req.getImapPort())
                .imapSsl(req.isImapSsl())
                .smtpHost(req.getSmtpHost())
                .smtpPort(req.getSmtpPort())
                .credential(encryptedCredential)
                .build();

        MailAccount savedAccount = mailAccountRepository.save(account);

        try {
            Long actorId = Long.parseLong(
                    SecurityContextHolder.getContext().getAuthentication().getName());
            auditLogService.record(actorId, "MAIL_ACCOUNT_CREATE", "MAIL_ACCOUNT",
                    savedAccount.getId(),
                    Map.of("emailAddress", savedAccount.getEmailAddress()),
                    true);
        } catch (Exception e) {
            log.warn("audit failed: {}", e.getMessage());
        }

        return MailAccountDto.Response.from(savedAccount);
    }

    public void delete(String spaceKey,
                       Long accountId,
                       Long userId,
                       String userRole,
                       List<Long> groupIds) {
        Space space = spaceRepository.findBySpaceKeyAndDeletedAtIsNull(spaceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        if (!permissionService.isSpaceAdmin(space.getId(), userId, userRole, groupIds)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        MailAccount account = mailAccountRepository.findByIdAndSpaceId(accountId, space.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        String emailAddress = account.getEmailAddress();
        mailAccountRepository.deleteById(account.getId());

        try {
            Long actorId = Long.parseLong(
                    SecurityContextHolder.getContext().getAuthentication().getName());
            auditLogService.record(actorId, "MAIL_ACCOUNT_DELETE", "MAIL_ACCOUNT",
                    accountId,
                    Map.of("emailAddress", emailAddress),
                    true);
        } catch (Exception e) {
            log.warn("audit failed: {}", e.getMessage());
        }
    }

    public void updateSyncStatus(Long accountId, String status) {
        MailAccount account = mailAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        account.setSyncStatus(status);
        mailAccountRepository.save(account);
    }
}
