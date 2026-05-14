package com.thecodinglab.imdbclone.engagement.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.ACCOUNT_ID;
import static com.thecodinglab.imdbclone.shared.logging.Log.RATING_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.catalog.api.MovieRatingAggregateService;
import com.thecodinglab.imdbclone.engagement.api.RatingRecord;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.engagement.internal.mapper.RatingMapper;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.shared.error.UnauthorizedException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import java.math.BigDecimal;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Ratings implements RatingService {

  private static final Logger logger = LoggerFactory.getLogger(Ratings.class);

  private final MovieReferenceService movieReferenceService;
  private final MovieRatingAggregateService movieRatingAggregateService;
  private final RatingRepository ratingRepository;
  private final RatingMapper ratingMapper;

  public Ratings(
      MovieReferenceService movieReferenceService,
      MovieRatingAggregateService movieRatingAggregateService,
      RatingRepository ratingRepository,
      RatingMapper ratingMapper) {
    this.movieReferenceService = movieReferenceService;
    this.movieRatingAggregateService = movieRatingAggregateService;
    this.ratingRepository = ratingRepository;
    this.ratingMapper = ratingMapper;
  }

  @Override
  @Transactional
  public RatingRecord rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score) {
    if (score.floatValue() < 0 || score.floatValue() > 10.1) {
      throw new BadRequestException("Score must be between 0 and 10");
    } else {
      movieReferenceService.findMovieById(movieId);
      Rating existingRating =
          ratingRepository
              .findByIdAccountIdAndIdMovieId(currentAccount.getId(), movieId)
              .orElse(null);
      BigDecimal ratingSumDelta =
          existingRating == null ? score : score.subtract(existingRating.getRating());
      int ratingCountDelta = existingRating == null ? 1 : 0;
      Rating rating = Rating.create(score, movieId, currentAccount.getId());
      Rating savedRating = ratingRepository.save(rating);
      movieRatingAggregateService.applyRatingAggregateDelta(
          movieId, ratingSumDelta, ratingCountDelta);
      logger.info("rating with [{}] was created.", kv(RATING_ID, savedRating.getId()));
      return ratingMapper.entityToDTO(savedRating);
    }
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
      throw new UnauthorizedException(
          "Account with id [%d] has no permission to delete this resource."
              .formatted(currentAccount.getId()));
    }
  }
}
