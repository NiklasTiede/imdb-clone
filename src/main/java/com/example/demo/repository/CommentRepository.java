package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
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
