package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.comment.CommentRecord;
import com.thecodinglab.imdbclone.payload.comment.CreateCommentRequest;
import com.thecodinglab.imdbclone.payload.comment.UpdateCommentRequest;
import com.thecodinglab.imdbclone.security.CurrentUser;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.CommentService;
import com.thecodinglab.imdbclone.validation.Pagination;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("/{commentId}")
  public ResponseEntity<CommentRecord> getCommentById(@PathVariable("commentId") Long commentId) {
    return new ResponseEntity<>(commentService.getComment(commentId), HttpStatus.OK);
  }

  @GetMapping("/{movieId}/comments")
  public ResponseEntity<Page<CommentRecord>> getCommentsByMovieId(
      @PathVariable("movieId") Long movieId,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    return new ResponseEntity<>(
        commentService.getCommentsByMovieId(movieId, page, size), HttpStatus.OK);
  }

  @PostMapping("/{movieId}")
  public ResponseEntity<CommentRecord> createComment(
      @PathVariable("movieId") Long movieId,
      @Valid @RequestBody CreateCommentRequest request,
      @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        commentService.createComment(movieId, request, currentAccount), HttpStatus.CREATED);
  }

  @PutMapping("/{commentId}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<CommentRecord> updateComment(
      @PathVariable("commentId") Long commentId,
      @Valid @RequestBody UpdateCommentRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        commentService.updateComment(commentId, request, currentAccount), HttpStatus.OK);
  }

  @DeleteMapping("/{commentId}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteComment(
      @PathVariable("commentId") Long commentId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        commentService.deleteComment(commentId, currentAccount), HttpStatus.NO_CONTENT);
  }
}
