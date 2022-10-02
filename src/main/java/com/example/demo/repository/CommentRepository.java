package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findCommentsByMovieOrderByCreatedAtInUtc(Movie movie);

  Page<Comment> findCommentsByAccountOrderByCreatedAtInUtc(Account account, Pageable pageable);

  default Comment getCommentById(Long commentId) {
    return findById(commentId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Comment with id [" + commentId + "] not found in database."));
  }
}
