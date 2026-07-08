package com.company.wiki.mail.repository;

import com.company.wiki.mail.entity.MailMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MailMessageAttachmentRepository extends JpaRepository<MailMessageAttachment, Long> {

    @Query("SELECT a.id, a.fileName, a.contentType, a.fileSize FROM MailMessageAttachment a WHERE a.mailMessageId = :messageId")
    List<Object[]> findMetaByMailMessageId(Long messageId);

    List<MailMessageAttachment> findByMailMessageId(Long mailMessageId);

    boolean existsByMailMessageId(Long mailMessageId);
}
