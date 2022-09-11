package com.example.demo.service.impl;

import com.example.demo.Payload.CreateCommentRequest;
import com.example.demo.Payload.MessageResponse;
import com.example.demo.Payload.UpdateCommentRequest;
import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.exceptions.UnauthorizedException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.MovieRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.CommentService;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommentServiceImpl.class);

  private final CommentRepository commentRepository;
  private final MovieRepository movieRepository;
  private final AccountRepository accountRepository;

  public CommentServiceImpl(
      CommentRepository commentRepository,
      MovieRepository movieRepository,
      AccountRepository accountRepository) {
    this.commentRepository = commentRepository;
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
  }

  @Override
  public Comment getComment(Long commentId) {
    Comment comment = commentRepository.getCommentById(commentId);
    LOGGER.info("comment with id [{}] was retrieved from database.", comment.getId());
    return comment;
  }

  @Override
  public List<Comment> getCommentsByMovieId(Long movieId) {
    Movie movie = movieRepository.getMovieById(movieId);
    List<Comment> comments = commentRepository.findCommentsByMovieOrderByCreatedAtInUtc(movie);
    if (comments.isEmpty()) {
      throw new NotFoundException(
          "Comments of movie with id [" + movie.getId() + "] not found in database.");
    }
    LOGGER.info("[{}] comments were retrieved from database.", comments.size());
    return comments;
  }

  @Override
  public List<Comment> getCommentsByAccount(UserPrincipal currentAccount) {
    Account account = accountRepository.getAccount(currentAccount);
    List<Comment> comments = commentRepository.findCommentsByAccountOrderByCreatedAtInUtc(account);
    if (comments.isEmpty()) {
      throw new NotFoundException(
          "Comments of account with id [" + account.getId() + "] not found in database.");
    }
    LOGGER.info("[{}] comments were retrieved from database.", comments.size());
    return comments;
  }

  @Override
  public Comment createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    Account account = accountRepository.getAccount(currentAccount);
    Comment comment = new Comment(request.message(), account, movie);
    Comment savedComment = commentRepository.save(comment);
    LOGGER.info("Comment with id [{}] was created", savedComment.getId());
    return savedComment;
  }

  @Override
  public Comment updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount) {
    Comment comment = commentRepository.getCommentById(commentId);
    if (Objects.equals(comment.getAccount().getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      comment.setMessage(request.message());
      Comment updatedComment = commentRepository.save(comment);
      LOGGER.info("comment with id [{}] was updated.", updatedComment.getId());
      return updatedComment;
    } else {
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to update this resource.");
    }
  }

  @Override
  public MessageResponse deleteComment(Long commentId, UserPrincipal currentAccount) {
    Comment comment = commentRepository.getCommentById(commentId);
    if (Objects.equals(comment.getAccount().getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      commentRepository.delete(comment);
      LOGGER.info("comment with id [{}] was deleted.", comment.getId());
      return new MessageResponse("comment with id [" + comment.getId() + "] was deleted.");
    } else {
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to delete this resource.");
    }
  }
}
