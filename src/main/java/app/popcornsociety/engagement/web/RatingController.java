package app.popcornsociety.engagement.web;

import app.popcornsociety.engagement.api.RatingRecord;
import app.popcornsociety.engagement.api.RatingRequest;
import app.popcornsociety.engagement.api.RatingScore;
import app.popcornsociety.engagement.api.RatingService;
import app.popcornsociety.shared.security.CurrentUser;
import app.popcornsociety.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/v{version}/accounts/me/ratings", version = "1")
public class RatingController {

  private final RatingService ratingService;

  public RatingController(RatingService ratingService) {
    this.ratingService = ratingService;
  }

  @PutMapping("/{movieId}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ApiResponse(
      responseCode = "200",
      description = "Existing resource updated",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "201", description = "Resource created", useReturnTypeSchema = true)
  public ResponseEntity<RatingRecord> rateMovie(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount,
      @PathVariable Long movieId,
      @Valid @RequestBody RatingRequest request) {
    var result = ratingService.rateMovie(currentAccount, movieId, RatingScore.of(request.score()));
    return result.created()
        ? ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
            .body(result.resource())
        : ResponseEntity.ok(result.resource());
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteRating(
      @PathVariable Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    ratingService.deleteRating(currentAccount, movieId);
    return ResponseEntity.noContent().build();
  }
}
