package com.company.wiki.content.repository;

import com.company.wiki.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ContentSearchBodyRepository extends JpaRepository<Content, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO content_search_bodies (content_id, search_vector) " +
            "VALUES (:contentId, to_tsvector('simple', coalesce(:body, ''))) " +
            "ON CONFLICT (content_id) DO UPDATE SET search_vector = to_tsvector('simple', coalesce(:body, ''))",
            nativeQuery = true)
    void upsertSearchBody(@Param("contentId") Long contentId, @Param("body") String body);
}
