package com.example.demo.controller;

import com.example.demo.payload.*;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.CommentService;
import com.example.demo.util.Pagination;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("/{commentId}")
  public ResponseEntity<CommentRecord> getCommentById(@PathVariable Long commentId) {
    return new ResponseEntity<>(commentService.getComment(commentId), HttpStatus.OK);
  }

  @GetMapping("/{movieId}/comments")
  public ResponseEntity<PagedResponse<CommentRecord>> getCommentsByMovieId(
      @PathVariable Long movieId,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER) Integer page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE) Integer size) {
    return new ResponseEntity<>(
        commentService.getCommentsByMovieId(movieId, page, size), HttpStatus.OK);
  }

  @PostMapping("/{movieId}")
  public ResponseEntity<CommentRecord> createComment(
      @PathVariable Long movieId,
      @RequestBody CreateCommentRequest request,
      @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        commentService.createComment(movieId, request, currentAccount), HttpStatus.CREATED);
  }

  @PutMapping("/{commentId}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<CommentRecord> updateComment(
      @PathVariable Long commentId,
      @RequestBody UpdateCommentRequest request,
      @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        commentService.updateComment(commentId, request, currentAccount), HttpStatus.OK);
  }

  @DeleteMapping("/{commentId}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteComment(
      @PathVariable Long commentId, @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        commentService.deleteComment(commentId, currentAccount), HttpStatus.OK);
  }
}
