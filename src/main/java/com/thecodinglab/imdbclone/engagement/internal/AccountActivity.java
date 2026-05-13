package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.engagement.api.AccountActivityService;
import com.thecodinglab.imdbclone.engagement.api.CommentRecord;
import com.thecodinglab.imdbclone.engagement.api.CommentService;
import com.thecodinglab.imdbclone.engagement.api.EngagementStats;
import com.thecodinglab.imdbclone.engagement.api.EngagementStatsService;
import com.thecodinglab.imdbclone.engagement.api.RatingRecord;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieService;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
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
