package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Comment;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.exception.domain.UnauthorizedException;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.comment.CommentRecord;
import com.thecodinglab.imdbclone.payload.comment.CreateCommentRequest;
import com.thecodinglab.imdbclone.payload.comment.UpdateCommentRequest;
import com.thecodinglab.imdbclone.payload.mapper.CustomCommentMapper;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.repository.CommentRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.CommentService;
import com.thecodinglab.imdbclone.validation.Pagination;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {

  private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

  private final CommentRepository commentRepository;
  private final MovieRepository movieRepository;
  private final AccountRepository accountRepository;
  private final CustomCommentMapper commentMapper;

  public CommentServiceImpl(
      CommentRepository commentRepository,
      MovieRepository movieRepository,
      AccountRepository accountRepository,
      CustomCommentMapper commentMapper) {
    this.commentRepository = commentRepository;
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
    this.commentMapper = commentMapper;
  }

  @Override
  public CommentRecord getComment(Long commentId) {
    Comment comment = commentRepository.getCommentById(commentId);
    CommentRecord commentRecord = commentMapper.entityToDTO(comment);
    logger.info("comment with [{}] was retrieved from database.", kv(COMMENT_ID, comment.getId()));
    return commentRecord;
  }

  @Override
  public Page<CommentRecord> getCommentsByMovieId(Long movieId, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Movie movie = movieRepository.getMovieById(movieId);
    Page<Comment> comments =
        commentRepository.findCommentsByMovieOrderByCreatedAtInUtc(movie, pageable);
    logger.info(
        "[{}] comments with commentIds [{}] by [{}] were retrieved from database.",
        comments.getContent().size(),
        v(COMMENT_IDS, comments.getContent().stream().map(Comment::getId).toList()),
        kv(MOVIE_ID, movieId));
    return comments.map(commentMapper::entityToDTO);
  }

  @Override
  public CommentRecord createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    Account account = accountRepository.getAccount(currentAccount);
    Comment comment = new Comment(request.message(), account, movie);
    Comment savedComment = commentRepository.save(comment);
    logger.info("Comment with [{}] was created", kv(COMMENT_ID, savedComment.getId()));
    return commentMapper.entityToDTO(comment);
  }

  @Override
  public Page<CommentRecord> getCommentsByAccount(String username, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Account account = accountRepository.getAccountByUsername(username);
    Page<Comment> comments =
        commentRepository.findCommentsByAccountOrderByCreatedAtInUtc(account, pageable);
    logger.info(
        "[{}] comments with commentIds [{}] of account [{}] were retrieved from database.",
        comments.getContent().size(),
        v(COMMENT_IDS, comments.getContent().stream().map(Comment::getId).toList()),
        username);
    return comments.map(commentMapper::entityToDTO);
  }

  @Override
  public CommentRecord updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount) {
    Comment comment = commentRepository.getCommentById(commentId);
    if (Objects.equals(comment.getAccount().getId(), currentAccount.getId())
        || Boolean.TRUE.equals(UserPrincipal.isCurrentAccountAdmin(currentAccount))) {
      comment.setMessage(request.message());
      Comment updatedComment = commentRepository.save(comment);
      logger.info("comment with [{}] was updated.", kv(COMMENT_ID, updatedComment.getId()));
      return commentMapper.entityToDTO(updatedComment);
    } else {
      throw new UnauthorizedException(
          "Account with id [%d] has no permission to update this resource."
              .formatted(currentAccount.getId()));
    }
  }

  @Override
  public MessageResponse deleteComment(Long commentId, UserPrincipal currentAccount) {
    Comment comment = commentRepository.getCommentById(commentId);
    if (Objects.equals(comment.getAccount().getId(), currentAccount.getId())
        || Boolean.TRUE.equals(UserPrincipal.isCurrentAccountAdmin(currentAccount))) {
      commentRepository.delete(comment);
      logger.info("comment with [{}] was deleted.", kv(COMMENT_ID, comment.getId()));
      return new MessageResponse("comment with id [%d] was deleted.".formatted(comment.getId()));
    } else {
      throw new UnauthorizedException(
          "Account with id [%d] has no permission to delete this resource."
              .formatted(currentAccount.getId()));
    }
  }
}
