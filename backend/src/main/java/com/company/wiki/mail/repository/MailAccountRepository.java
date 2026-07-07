package com.company.wiki.mail.repository;

import com.company.wiki.mail.entity.MailAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

    List<MailAccount> findBySpaceId(Long spaceId);

    List<MailAccount> findBySyncStatus(String syncStatus);

    long countBySyncStatus(String syncStatus);

    Optional<MailAccount> findByIdAndSpaceId(Long id, Long spaceId);
}
