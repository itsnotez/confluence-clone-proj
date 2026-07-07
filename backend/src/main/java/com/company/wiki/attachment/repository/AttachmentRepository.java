package com.company.wiki.attachment.repository;

import com.company.wiki.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByContentId(Long contentId);

    @Query("SELECT COALESCE(SUM(a.sizeBytes), 0) FROM Attachment a")
    long sumSizeBytes();
}
