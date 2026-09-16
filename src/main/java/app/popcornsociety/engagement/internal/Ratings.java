package app.popcornsociety.engagement.internal;

import static app.popcornsociety.shared.logging.Log.ACCOUNT_ID;
import static app.popcornsociety.shared.logging.Log.RATING_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import app.popcornsociety.catalog.api.MovieRatingAggregateService;
import app.popcornsociety.catalog.api.MovieReferenceService;
import app.popcornsociety.engagement.api.AccountEngagementLifecycle;
import app.popcornsociety.engagement.api.RatingRecord;
import app.popcornsociety.engagement.api.RatingScore;
import app.popcornsociety.engagement.api.RatingService;
import app.popcornsociety.engagement.internal.mapper.RatingMapper;
import app.popcornsociety.engagement.internal.persistence.Rating;
import app.popcornsociety.engagement.internal.persistence.RatingAccountLock;
import app.popcornsociety.engagement.internal.persistence.RatingRepository;
import app.popcornsociety.shared.api.MessageResponse;
import app.popcornsociety.shared.api.PagedResponse;
import app.popcornsociety.shared.api.ResourceWriteResult;
import app.popcornsociety.shared.error.NotFoundException;
import app.popcornsociety.shared.security.UserPrincipal;
import app.popcornsociety.shared.validation.Pagination;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Ratings implements RatingService, AccountEngagementLifecycle {

  private static final Logger logger = LoggerFactory.getLogger(Ratings.class);
  private final MovieReferenceService movieReferenceService;
  private final MovieRatingAggregateService movieRatingAggregateService;
  private final RatingRepository ratingRepository;
  private final RatingMapper ratingMapper;
  private final RatingAccountLock accountLock;

  public Ratings(
      MovieReferenceService movieReferenceService,
      MovieRatingAggregateService movieRatingAggregateService,
      RatingRepository ratingRepository,
      RatingMapper ratingMapper,
      RatingAccountLock accountLock) {
    this.movieReferenceService = movieReferenceService;
    this.movieRatingAggregateService = movieRatingAggregateService;
    this.ratingRepository = ratingRepository;
    this.ratingMapper = ratingMapper;
    this.accountLock = accountLock;
  }

  @Override
  @Transactional
  public ResourceWriteResult<RatingRecord> rateMovie(
      UserPrincipal currentAccount, Long movieId, RatingScore score) {
    return rateForAccount(currentAccount.getId(), movieId, score);
  }

  @Transactional
  public ResourceWriteResult<RatingRecord> rateForAccount(
      Long accountId, Long movieId, RatingScore score) {
    accountLock.acquire(accountId);
    movieReferenceService.findMovieById(movieId);
    Rating existingRating =
        ratingRepository.findByIdAccountIdAndIdMovieId(accountId, movieId).orElse(null);
    BigDecimal scoreValue = score.value();
    BigDecimal ratingSumDelta =
        existingRating == null ? scoreValue : scoreValue.subtract(existingRating.getRating());
    int ratingCountDelta = existingRating == null ? 1 : 0;
    Rating rating = Rating.create(scoreValue, movieId, accountId);
    Rating savedRating = ratingRepository.save(rating);
    movieRatingAggregateService.applyRatingAggregateDelta(
        movieId, ratingSumDelta, ratingCountDelta);
    logger.info("rating with [{}] was created.", kv(RATING_ID, savedRating.getId()));
    return new ResourceWriteResult<>(ratingMapper.entityToDTO(savedRating), existingRating == null);
  }

  @Override
  public PagedResponse<RatingRecord> getRatingsByAccountId(Long accountId, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Page<Rating> ratings = ratingRepository.findRatingsByIdAccountId(accountId, pageable);
    logger.info(
        "[{}] ratings from account with [{}] were retrieved.",
        ratings.getContent().size(),
        kv(ACCOUNT_ID, accountId));
    return PagedResponse.from(ratings.map(ratingMapper::entityToDTO));
  }

  @Override
  @Transactional
  public MessageResponse deleteRating(UserPrincipal currentAccount, Long movieId) {
    accountLock.acquire(currentAccount.getId());
    Rating rating =
        ratingRepository
            .findByIdAccountIdAndIdMovieId(currentAccount.getId(), movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Rating with movieId [%d] and accountId [%d] not found in database."
                            .formatted(movieId, currentAccount.getId())));
    if (Objects.equals(rating.getAccountId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      ratingRepository.delete(rating);
      movieRatingAggregateService.applyRatingAggregateDelta(
          movieId, rating.getRating().negate(), -1);
      logger.info("rating with [{}] was deleted.", kv(RATING_ID, rating.getId()));
      return new MessageResponse(
          "WatchedMovie with movieId [%d] and accountId [%d] was deleted"
              .formatted(movieId, currentAccount.getId()));
    } else {
      throw new AccessDeniedException(
          "Account with id [%d] has no permission to delete this resource."
              .formatted(currentAccount.getId()));
    }
  }

  @Transactional
  public BigDecimal removeForAccount(Long accountId, Long movieId) {
    accountLock.acquire(accountId);
    var rating = ratingRepository.findByIdAccountIdAndIdMovieId(accountId, movieId).orElse(null);
    if (rating == null) return null;
    ratingRepository.delete(rating);
    movieRatingAggregateService.applyRatingAggregateDelta(movieId, rating.getRating().negate(), -1);
    return rating.getRating();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void removeAccountRatings(Long accountId) {
    accountLock.acquire(accountId);
    // A stable movie order avoids opposing lock order when two accounts are removed concurrently.
    var accountRatings =
        ratingRepository.findAllByIdAccountId(accountId).stream()
            .sorted(Comparator.comparing(Rating::getMovieId))
            .toList();
    for (Rating rating : accountRatings) {
      ratingRepository.delete(rating);
      movieRatingAggregateService.applyRatingAggregateDelta(
          rating.getMovieId(), rating.getRating().negate(), -1);
    }
  }
}
