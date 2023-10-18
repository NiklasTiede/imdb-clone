package com.thecodinglab.imdbclone.job;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.entity.Rating;
import com.thecodinglab.imdbclone.entity.VerificationToken;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.repository.RatingRepository;
import com.thecodinglab.imdbclone.repository.VerificationTokenRepository;
import com.thecodinglab.imdbclone.service.MovieService;
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
public class ScheduledTasks {

  private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

  private final RatingRepository ratingRepository;
  private final MovieRepository movieRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final MovieService movieService;

  public ScheduledTasks(
      RatingRepository ratingRepository,
      MovieRepository movieRepository,
      VerificationTokenRepository verificationTokenRepository,
      MovieService movieService) {
    this.ratingRepository = ratingRepository;
    this.movieRepository = movieRepository;
    this.verificationTokenRepository = verificationTokenRepository;
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
    } else {
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
            new BigDecimal(averageRating, new MathContext(2, RoundingMode.HALF_EVEN));

        Movie movie = movieRepository.getMovieById(movieRating.get(0).getMovie().getId());
        movie.setRating(newRating);
        movie.setRatingCount(countOfAllRatings);
        Movie savedMovie = movieService.performSave(movie);

        logger.info(
            "movie [{}] was updated. The new average rating is [{}] with [{}] counts",
            savedMovie.getPrimaryTitle(),
            savedMovie.getRating(),
            savedMovie.getRatingCount());
      }
      logger.info("The rating / ratingCount of [{}] movie(s) were updated", ratingsOfMovies.size());
    }
  }

  /** Cleaning job deleting 30-day old tokens. The job is scheduled at 1:10 AM */
  @Scheduled(cron = "0 10 1 * * *")
  public void deleteExpiredVerificationTokens() {
    List<VerificationToken> oldExpiredVerificationTokens =
        verificationTokenRepository.findAllByExpiryDateInUtcBefore(
            Instant.now().minus(30, ChronoUnit.DAYS));
    Integer oldTokensCount = oldExpiredVerificationTokens.size();
    verificationTokenRepository.deleteAll(oldExpiredVerificationTokens);
    logger.info(
        "[{}] expired verification tokens older than 4 weeks were deleted ", oldTokensCount);
  }
}
