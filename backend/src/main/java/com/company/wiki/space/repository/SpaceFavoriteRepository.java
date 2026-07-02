package com.company.wiki.space.repository;

import com.company.wiki.space.entity.SpaceFavorite;
import com.company.wiki.space.entity.SpaceFavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SpaceFavoriteRepository extends JpaRepository<SpaceFavorite, SpaceFavoriteId> {

    boolean existsByIdSpaceIdAndIdUserId(Long spaceId, Long userId);

    @Modifying
    @Query("DELETE FROM SpaceFavorite sf WHERE sf.id.spaceId = :spaceId AND sf.id.userId = :userId")
    void deleteBySpaceIdAndUserId(@Param("spaceId") Long spaceId, @Param("userId") Long userId);

    List<SpaceFavorite> findByIdUserId(Long userId);
}
