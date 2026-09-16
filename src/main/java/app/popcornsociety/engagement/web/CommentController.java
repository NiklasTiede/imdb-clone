package app.popcornsociety.engagement.web;

import app.popcornsociety.engagement.api.CommentRecord;
import app.popcornsociety.engagement.api.CommentService;
import app.popcornsociety.engagement.api.CreateCommentRequest;
import app.popcornsociety.engagement.api.UpdateCommentRequest;
import app.popcornsociety.shared.api.PagedResponse;
import app.popcornsociety.shared.security.CurrentUser;
import app.popcornsociety.shared.security.UserPrincipal;
import app.popcornsociety.shared.validation.Pagination;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/v{version}", version = "1")
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("/comments/{commentId}")
  public ResponseEntity<CommentRecord> getCommentById(@PathVariable Long commentId) {
    return new ResponseEntity<>(commentService.getComment(commentId), HttpStatus.OK);
  }

  @GetMapping("/movies/{movieId}/comments")
  public ResponseEntity<PagedResponse<CommentRecord>> getCommentsByMovieId(
      @PathVariable Long movieId,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    return new ResponseEntity<>(
        commentService.getCommentsByMovieId(movieId, page, size), HttpStatus.OK);
  }

  @PostMapping("/movies/{movieId}/comments")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<CommentRecord> createComment(
      @PathVariable Long movieId,
      @Valid @RequestBody CreateCommentRequest request,
      @CurrentUser UserPrincipal currentAccount) {
    CommentRecord comment = commentService.createComment(movieId, request, currentAccount);
    return ResponseEntity.created(
            ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/comments/{id}")
                .buildAndExpand(comment.id())
                .toUri())
        .body(comment);
  }

  @PutMapping("/comments/{commentId}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<CommentRecord> updateComment(
      @PathVariable Long commentId,
      @Valid @RequestBody UpdateCommentRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        commentService.updateComment(commentId, request, currentAccount), HttpStatus.OK);
  }

  @DeleteMapping("/comments/{commentId}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteComment(
      @PathVariable Long commentId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    commentService.deleteComment(commentId, currentAccount);
    return ResponseEntity.noContent().build();
  }
}
