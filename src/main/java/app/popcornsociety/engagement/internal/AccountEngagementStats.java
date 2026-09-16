package app.popcornsociety.engagement.internal;

import app.popcornsociety.engagement.api.EngagementStats;
import app.popcornsociety.engagement.api.EngagementStatsService;
import app.popcornsociety.engagement.internal.persistence.CommentRepository;
import app.popcornsociety.engagement.internal.persistence.RatingRepository;
import app.popcornsociety.engagement.internal.persistence.WatchedMovieRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountEngagementStats implements EngagementStatsService {

  private final RatingRepository ratingRepository;
  private final WatchedMovieRepository watchedMovieRepository;
  private final CommentRepository commentRepository;

  public AccountEngagementStats(
      RatingRepository ratingRepository,
      WatchedMovieRepository watchedMovieRepository,
      CommentRepository commentRepository) {
    this.ratingRepository = ratingRepository;
    this.watchedMovieRepository = watchedMovieRepository;
    this.commentRepository = commentRepository;
  }

  @Override
  public EngagementStats getStatsForAccount(Long accountId) {
    return new EngagementStats(
        ratingRepository.countByIdAccountId(accountId),
        watchedMovieRepository.countByIdAccountId(accountId),
        commentRepository.countCommentsByAccountId(accountId));
  }
}
