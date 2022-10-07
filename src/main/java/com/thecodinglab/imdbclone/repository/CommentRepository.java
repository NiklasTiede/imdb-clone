package com.thecodinglab.imdbclone.repository;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Comment;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  Page<Comment> findCommentsByMovieOrderByCreatedAtInUtc(Movie movie, Pageable pageable);

  Page<Comment> findCommentsByAccountOrderByCreatedAtInUtc(Account account, Pageable pageable);

  Long countCommentsByAccount(Account account);

  default Comment getCommentById(Long commentId) {
    return findById(commentId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Comment with id [" + commentId + "] not found in database."));
  }
}
