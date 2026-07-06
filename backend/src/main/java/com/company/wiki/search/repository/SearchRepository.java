package com.company.wiki.search.repository;

import com.company.wiki.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchRepository extends JpaRepository<Content, Long> {

    @Query(value = """
            SELECT c.id, c.title, c.space_id AS spaceId, c.status,
                   c.updated_at AS updatedAt,
                   ts_rank(c.search_vector, websearch_to_tsquery('simple', :q)) AS rank
            FROM contents c
            WHERE c.deleted_at IS NULL
              AND c.search_vector @@ websearch_to_tsquery('simple', :q)
            UNION ALL
            SELECT c.id, c.title, c.space_id, c.status, c.updated_at,
                   ts_rank(csb.search_vector, websearch_to_tsquery('simple', :q)) AS rank
            FROM content_search_bodies csb
            JOIN contents c ON csb.content_id = c.id
            WHERE c.deleted_at IS NULL
              AND csb.search_vector @@ websearch_to_tsquery('simple', :q)
            ORDER BY rank DESC LIMIT 50
            """, nativeQuery = true)
    List<Object[]> searchContents(@Param("q") String q);
}
