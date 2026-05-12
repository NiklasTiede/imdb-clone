package com.thecodinglab.imdbclone.engagement.internal;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.engagement.api.CommentRecord;
import com.thecodinglab.imdbclone.engagement.api.CommentService;
import com.thecodinglab.imdbclone.engagement.api.CreateCommentRequest;
import com.thecodinglab.imdbclone.engagement.api.UpdateCommentRequest;
import com.thecodinglab.imdbclone.engagement.internal.mapper.CommentMapper;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Comment;
import com.thecodinglab.imdbclone.engagement.internal.persistence.CommentRepository;
import com.thecodinglab.imdbclone.exception.domain.UnauthorizedException;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.validation.Pagination;
import jakarta.persistence.EntityManager;
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
  private final MovieService movieService;
  private final EntityManager entityManager;
  private final CommentMapper commentMapper;

  public CommentServiceImpl(
      CommentRepository commentRepository,
      MovieService movieService,
      EntityManager entityManager,
      CommentMapper commentMapper) {
    this.commentRepository = commentRepository;
    this.movieService = movieService;
    this.entityManager = entityManager;
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
  public PagedResponse<CommentRecord> getCommentsByMovieId(Long movieId, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    movieService.findMovieById(movieId);
    Page<Comment> comments =
        commentRepository.findCommentsByMovieIdOrderByCreatedAtInUtc(movieId, pageable);
    logger.info(
        "[{}] comments with commentIds [{}] by [{}] were retrieved from database.",
        comments.getContent().size(),
        v(COMMENT_IDS, comments.getContent().stream().map(Comment::getId).toList()),
        kv(MOVIE_ID, movieId));
    return PagedResponse.from(comments.map(commentMapper::entityToDTO));
  }

  @Override
  public CommentRecord createComment(
      Long movieId, CreateCommentRequest request, UserPrincipal currentAccount) {
    movieService.findMovieById(movieId);
    Movie movie = entityManager.getReference(Movie.class, movieId);
    Comment comment = new Comment(request.message(), currentAccount.getId(), movie);
    Comment savedComment = commentRepository.save(comment);
    logger.info("Comment with [{}] was created", kv(COMMENT_ID, savedComment.getId()));
    return commentMapper.entityToDTO(comment);
  }

  @Override
  public PagedResponse<CommentRecord> getCommentsByAccountId(Long accountId, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Page<Comment> comments =
        commentRepository.findCommentsByAccountIdOrderByCreatedAtInUtc(accountId, pageable);
    logger.info(
        "[{}] comments with commentIds [{}] of account [{}] were retrieved from database.",
        comments.getContent().size(),
        v(COMMENT_IDS, comments.getContent().stream().map(Comment::getId).toList()),
        kv(ACCOUNT_ID, accountId));
    return PagedResponse.from(comments.map(commentMapper::entityToDTO));
  }

  @Override
  public CommentRecord updateComment(
      Long commentId, UpdateCommentRequest request, UserPrincipal currentAccount) {
    Comment comment = commentRepository.getCommentById(commentId);
    if (Objects.equals(comment.getAccountId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
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
    if (Objects.equals(comment.getAccountId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
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
