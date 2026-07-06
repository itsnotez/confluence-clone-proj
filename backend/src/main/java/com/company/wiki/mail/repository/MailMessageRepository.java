package com.company.wiki.mail.repository;

import com.company.wiki.mail.entity.MailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailMessageRepository extends JpaRepository<MailMessage, Long> {

    boolean existsByMailAccountIdAndMessageUid(Long mailAccountId, String messageUid);

    List<MailMessage> findByMailAccountIdOrderByReceivedAtDesc(Long mailAccountId);

    Optional<MailMessage> findByIdAndMailAccountId(Long id, Long mailAccountId);
}
