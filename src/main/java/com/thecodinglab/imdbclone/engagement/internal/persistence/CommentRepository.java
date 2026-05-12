package com.thecodinglab.imdbclone.engagement.internal.persistence;

import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  Page<Comment> findCommentsByMovieIdOrderByCreatedAtInUtc(Long movieId, Pageable pageable);

  Page<Comment> findCommentsByAccountIdOrderByCreatedAtInUtc(Long accountId, Pageable pageable);

  Long countCommentsByAccountId(Long accountId);

  default Comment getCommentById(Long commentId) {
    return findById(commentId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Comment with id [" + commentId + "] not found in database."));
  }
}
