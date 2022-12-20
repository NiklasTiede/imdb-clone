package com.thecodinglab.imdbclone.service.impl;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Comment;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.exception.UnauthorizedException;
import com.thecodinglab.imdbclone.payload.*;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(CommentServiceImpl.class);

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
    LOGGER.info("comment with id [{}] was retrieved from database.", comment.getId());
    return commentRecord;
  }

  @Override
  public PagedResponse<CommentRecord> getCommentsByMovieId(Long movieId, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Movie movie = movieRepository.getMovieById(movieId);
    Page<Comment> comments =
        commentRepository.findCommentsByMovieOrderByCreatedAtInUtc(movie, pageable);
    LOGGER.info("[{}] comments were retrieved from database.", comments.getContent().size());
    Page<CommentRecord> commentRecordPage = comments.map(commentMapper::entityToDTO);
    return new PagedResponse<>(
        commentRecordPage.getContent(),
        commentRecordPage.getNumber(),
        commentRecordPage.getSize(),
        commentRecordPage.getTotalElements(),
        commentRecordPage.getTotalPages(),
        commentRecordPage.isLast());
  }

  @Override
  public CommentRecord createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    Account account = accountRepository.getAccount(currentAccount);
    Comment comment = new Comment(request.message(), account, movie);
    Comment savedComment = commentRepository.save(comment);
    LOGGER.info("Comment with id [{}] was created", savedComment.getId());
    return commentMapper.entityToDTO(comment);
  }

  @Override
  public PagedResponse<CommentRecord> getCommentsByAccount(String username, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Account account = accountRepository.getAccountByUsername(username);
    Page<Comment> comments =
        commentRepository.findCommentsByAccountOrderByCreatedAtInUtc(account, pageable);
    LOGGER.info("[{}] comments were retrieved from database.", comments.getContent().size());
    Page<CommentRecord> commentRecordPage = comments.map(commentMapper::entityToDTO);
    return new PagedResponse<>(
        commentRecordPage.getContent(),
        commentRecordPage.getNumber(),
        commentRecordPage.getSize(),
        commentRecordPage.getTotalElements(),
        commentRecordPage.getTotalPages(),
        commentRecordPage.isLast());
  }

  @Override
  public CommentRecord updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount) {
    Comment comment = commentRepository.getCommentById(commentId);
    if (Objects.equals(comment.getAccount().getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      comment.setMessage(request.message());
      Comment updatedComment = commentRepository.save(comment);
      LOGGER.info("comment with id [{}] was updated.", updatedComment.getId());
      return commentMapper.entityToDTO(updatedComment);
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
