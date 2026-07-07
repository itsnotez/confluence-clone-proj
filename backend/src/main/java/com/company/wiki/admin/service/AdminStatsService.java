package com.company.wiki.admin.service;

import com.company.wiki.admin.dto.AdminStatsDto;
import com.company.wiki.attachment.repository.AttachmentRepository;
import com.company.wiki.content.repository.ContentRepository;
import com.company.wiki.mail.repository.MailAccountRepository;
import com.company.wiki.space.repository.SpaceRepository;
import com.company.wiki.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final ContentRepository contentRepository;
    private final AttachmentRepository attachmentRepository;
    private final MailAccountRepository mailAccountRepository;

    public AdminStatsDto getStats() {
        long activeUsers = userRepository.countActiveUsers();
        long totalSpaces = spaceRepository.countActiveSpaces();
        long totalContents = contentRepository.countActiveContents();
        long storageUsedBytes = attachmentRepository.sumSizeBytes();
        long mailAccountsOk = mailAccountRepository.countBySyncStatus("ACTIVE");
        long mailAccountsFailed = mailAccountRepository.countBySyncStatus("DISABLED");

        return AdminStatsDto.builder()
                .activeUsers(activeUsers)
                .totalSpaces(totalSpaces)
                .totalContents(totalContents)
                .storageUsedBytes(storageUsedBytes)
                .mailAccountsOk(mailAccountsOk)
                .mailAccountsFailed(mailAccountsFailed)
                .build();
    }
}
