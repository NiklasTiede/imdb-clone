package app.popcornsociety.engagement.internal;

import app.popcornsociety.engagement.api.AccountActivityService;
import app.popcornsociety.engagement.api.CommentRecord;
import app.popcornsociety.engagement.api.CommentService;
import app.popcornsociety.engagement.api.EngagementStats;
import app.popcornsociety.engagement.api.EngagementStatsService;
import app.popcornsociety.engagement.api.RatingRecord;
import app.popcornsociety.engagement.api.RatingService;
import app.popcornsociety.engagement.api.WatchedMovieRecord;
import app.popcornsociety.engagement.api.WatchedMovieService;
import app.popcornsociety.shared.api.PagedResponse;
import org.springframework.stereotype.Service;

@Service
public class AccountActivity implements AccountActivityService {

  private final CommentService commentService;
  private final WatchedMovieService watchedMovieService;
  private final RatingService ratingService;
  private final EngagementStatsService engagementStatsService;

  public AccountActivity(
      CommentService commentService,
      WatchedMovieService watchedMovieService,
      RatingService ratingService,
      EngagementStatsService engagementStatsService) {
    this.commentService = commentService;
    this.watchedMovieService = watchedMovieService;
    this.ratingService = ratingService;
    this.engagementStatsService = engagementStatsService;
  }

  @Override
  public EngagementStats getStatsForAccount(Long accountId) {
    return engagementStatsService.getStatsForAccount(accountId);
  }

  @Override
  public PagedResponse<CommentRecord> getCommentsByAccountId(Long accountId, int page, int size) {
    return commentService.getCommentsByAccountId(accountId, page, size);
  }

  @Override
  public PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccountId(
      Long accountId, int page, int size) {
    return watchedMovieService.getWatchedMoviesByAccountId(accountId, page, size);
  }

  @Override
  public PagedResponse<RatingRecord> getRatingsByAccountId(Long accountId, int page, int size) {
    return ratingService.getRatingsByAccountId(accountId, page, size);
  }
}
