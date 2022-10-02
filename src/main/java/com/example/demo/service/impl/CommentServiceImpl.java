package com.example.demo.service.impl;

import com.example.demo.Payload.*;
import com.example.demo.Payload.mapper.CustomCommentMapper;
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
import com.example.demo.util.Pagination;
import java.util.List;
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
  public List<CommentRecord> getCommentsByMovieId(Long movieId) {
    Movie movie = movieRepository.getMovieById(movieId);
    System.out.println(movie.getPrimaryTitle());
    List<Comment> comments = commentRepository.findCommentsByMovieOrderByCreatedAtInUtc(movie);
    System.out.println(comments);
    if (comments.isEmpty()) {
      throw new NotFoundException(
          "No comments of movie with id [" + movie.getId() + "] found in database.");
    }
    List<CommentRecord> commentRecords = commentMapper.entityToDTO(comments);
    LOGGER.info("[{}] comments were retrieved from database.", comments.size());
    return commentRecords;
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
    Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAtInUtc");
    Account account = accountRepository.getAccountByName(username);
    Page<Comment> comments =
        commentRepository.findCommentsByAccountOrderByCreatedAtInUtc(account, pageable);
    if (comments.getContent().isEmpty()) {
      throw new NotFoundException(
          "Comments of account with id [" + account.getId() + "] not found in database.");
    }
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
