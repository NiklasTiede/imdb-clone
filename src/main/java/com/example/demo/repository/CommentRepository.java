package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findCommentsByMovieOrderByCreatedAtInUtc(Movie movie);

  List<Comment> findCommentsByAccountOrderByCreatedAtInUtc(Account account);

  default Comment getCommentById(Long commentId) {
    return findById(commentId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Comment with id [" + commentId + "] not found in database."));
  }
}
