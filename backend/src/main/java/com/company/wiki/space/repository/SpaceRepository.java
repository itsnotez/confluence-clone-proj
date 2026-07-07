package com.company.wiki.space.repository;

import com.company.wiki.space.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    Optional<Space> findBySpaceKeyAndDeletedAtIsNull(String spaceKey);

    List<Space> findByStatusAndDeletedAtIsNull(String status);

    boolean existsBySpaceKey(String spaceKey);

    @Query("SELECT COUNT(s) FROM Space s WHERE s.deletedAt IS NULL")
    long countActiveSpaces();
}
