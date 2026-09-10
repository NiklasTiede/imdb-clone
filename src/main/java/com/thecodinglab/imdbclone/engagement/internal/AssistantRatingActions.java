package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.engagement.api.AssistantActionReceipt;
import com.thecodinglab.imdbclone.engagement.api.AssistantRatings;
import com.thecodinglab.imdbclone.engagement.api.RatingScore;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingAccountLock;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantRatingActions implements AssistantRatings {
  private final Ratings ratings;
  private final RatingRepository repository;
  private final RatingAccountLock lock;
  private final AssistantActionReceipts receipts;

  public AssistantRatingActions(
      Ratings ratings,
      RatingRepository repository,
      RatingAccountLock lock,
      AssistantActionReceipts receipts) {
    this.ratings = ratings;
    this.repository = repository;
    this.lock = lock;
    this.receipts = receipts;
  }

  @Override
  @Transactional
  public AssistantActionReceipt rate(
      Long accountId, Long movieId, BigDecimal score, UUID operationId) {
    var validated = new RatingScore(score);
    return receipts.execute(
        accountId,
        movieId,
        operationId,
        "rating_set",
        validated.value(),
        () -> {
          lock.acquire(accountId);
          var previous =
              repository
                  .findByIdAccountIdAndIdMovieId(accountId, movieId)
                  .map(rating -> rating.getRating())
                  .orElse(null);
          boolean changed = previous == null || previous.compareTo(validated.value()) != 0;
          if (changed) ratings.rateForAccount(accountId, movieId, validated);
          return new AssistantActionReceipt(
              operationId,
              movieId,
              "rating_set",
              changed,
              validated.value(),
              previous,
              Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        });
  }

  @Override
  @Transactional
  public AssistantActionReceipt remove(Long accountId, Long movieId, UUID operationId) {
    return receipts.execute(
        accountId,
        movieId,
        operationId,
        "rating_remove",
        null,
        () -> {
          var previous = ratings.removeForAccount(accountId, movieId);
          return new AssistantActionReceipt(
              operationId,
              movieId,
              "rating_remove",
              previous != null,
              null,
              previous,
              Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        });
  }
}
