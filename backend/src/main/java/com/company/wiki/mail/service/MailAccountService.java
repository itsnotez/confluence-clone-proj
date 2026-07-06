package com.company.wiki.mail.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MailAccountService {

    private final SpaceRepository spaceRepository;
    private final MailAccountRepository mailAccountRepository;
    private final AesEncryptUtil aesEncryptUtil;
    private final PermissionService permissionService;

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

        return MailAccountDto.Response.from(mailAccountRepository.save(account));
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

        mailAccountRepository.deleteById(account.getId());
    }

    public void updateSyncStatus(Long accountId, String status) {
        MailAccount account = mailAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_ACCOUNT_NOT_FOUND));

        account.setSyncStatus(status);
        mailAccountRepository.save(account);
    }
}
