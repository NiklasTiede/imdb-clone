package com.thecodinglab.imdbclone.engagement.internal;

import static com.thecodinglab.imdbclone.utility.Log.ACCOUNT_ID;
import static com.thecodinglab.imdbclone.utility.Log.RATING_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.engagement.api.RatingRecord;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.engagement.internal.mapper.RatingMapper;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.exception.domain.BadRequestException;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import com.thecodinglab.imdbclone.exception.domain.UnauthorizedException;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.validation.Pagination;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class RatingServiceImpl implements RatingService {

  private static final Logger logger = LoggerFactory.getLogger(RatingServiceImpl.class);

  private final MovieService movieService;
  private final EntityManager entityManager;
  private final RatingRepository ratingRepository;
  private final RatingMapper ratingMapper;

  public RatingServiceImpl(
      MovieService movieService,
      EntityManager entityManager,
      RatingRepository ratingRepository,
      RatingMapper ratingMapper) {
    this.movieService = movieService;
    this.entityManager = entityManager;
    this.ratingRepository = ratingRepository;
    this.ratingMapper = ratingMapper;
  }

  @Override
  public RatingRecord rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score) {
    if (score.floatValue() < 0 || score.floatValue() > 10.1) {
      throw new BadRequestException("Score must be between 0 and 10");
    } else {
      movieService.findMovieById(movieId);
      Movie movie = entityManager.getReference(Movie.class, movieId);
      Rating rating = Rating.create(score, movie, currentAccount.getId());
      Rating savedRating = ratingRepository.save(rating);
      logger.info("rating with [{}] was created.", kv(RATING_ID, savedRating.getId()));
      return new RatingRecord(
          savedRating.getRating(), savedRating.getAccountId(), savedRating.getMovie().getId());
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
