package com.example.demo.controller;

import com.example.demo.Payload.CommentRecord;
import com.example.demo.Payload.CreateCommentRequest;
import com.example.demo.Payload.MessageResponse;
import com.example.demo.Payload.UpdateCommentRequest;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.CommentService;
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
  public ResponseEntity<CommentRecord> findCommentById(@PathVariable Long commentId) {
    return new ResponseEntity<>(commentService.getComment(commentId), HttpStatus.OK);
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
