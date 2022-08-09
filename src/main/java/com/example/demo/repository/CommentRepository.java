package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findCommentsByMovie(Movie movie);

  List<Comment> findCommentsByAccount(Account account);
}
