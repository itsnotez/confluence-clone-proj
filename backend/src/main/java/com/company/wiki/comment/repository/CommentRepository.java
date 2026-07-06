package com.company.wiki.comment.repository;

import com.company.wiki.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByContentIdAndDeletedAtIsNull(Long contentId);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
}
