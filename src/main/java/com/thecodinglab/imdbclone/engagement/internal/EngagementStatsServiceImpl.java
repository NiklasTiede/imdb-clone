package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.engagement.api.EngagementStats;
import com.thecodinglab.imdbclone.engagement.api.EngagementStatsService;
import com.thecodinglab.imdbclone.engagement.internal.persistence.CommentRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import org.springframework.stereotype.Service;

@Service
public class EngagementStatsServiceImpl implements EngagementStatsService {

  private final RatingRepository ratingRepository;
  private final WatchedMovieRepository watchedMovieRepository;
  private final CommentRepository commentRepository;

  public EngagementStatsServiceImpl(
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
