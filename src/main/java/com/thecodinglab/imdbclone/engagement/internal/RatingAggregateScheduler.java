package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RatingAggregateScheduler {

  private static final Logger logger = LoggerFactory.getLogger(RatingAggregateScheduler.class);

  private final RatingRepository ratingRepository;
  private final MovieService movieService;

  public RatingAggregateScheduler(RatingRepository ratingRepository, MovieService movieService) {
    this.ratingRepository = ratingRepository;
    this.movieService = movieService;
  }

  /**
   * If a user created a new rating for a movie in the last 24h the movie's rating will be updated.
   * The job is scheduled at 1:00 AM
   */
  @Scheduled(cron = "0 0 1 * * *")
  public void updateMovieRatings() {
    List<Rating> recentlyCreatedRatings =
        ratingRepository.findAllByModifiedAtInUtcAfter(Instant.now().minus(24, ChronoUnit.HOURS));
    if (recentlyCreatedRatings.isEmpty()) {
      logger.info("no newly created ratings in the last 24h, no movie to update");
      return;
    }

    List<List<Rating>> ratingsOfMovies =
        recentlyCreatedRatings.stream()
            .map(rating -> rating.getMovie().getId())
            .map(ratingRepository::findRatingsByMovieId)
            .toList();

    for (List<Rating> movieRating : ratingsOfMovies) {
      double sumOfAllRatings =
          movieRating.stream().map(Rating::getRating).mapToDouble(BigDecimal::doubleValue).sum();
      int countOfAllRatings = movieRating.size();
      double averageRating = sumOfAllRatings / countOfAllRatings;

      BigDecimal newRating =
          new BigDecimal(String.valueOf(averageRating), new MathContext(2, RoundingMode.HALF_EVEN));

      MovieRecord savedMovie =
          movieService.updateRatingAggregate(
              movieRating.getFirst().getMovie().getId(), newRating, countOfAllRatings);

      logger.info(
          "movie [{}] was updated. The new average rating is [{}] with [{}] counts",
          savedMovie.primaryTitle(),
          savedMovie.rating(),
          savedMovie.ratingCount());
    }
    logger.info("The rating / ratingCount of [{}] movie(s) were updated", ratingsOfMovies.size());
  }
}
