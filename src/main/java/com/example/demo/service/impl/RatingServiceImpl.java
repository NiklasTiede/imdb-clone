package com.example.demo.service.impl;

import com.example.demo.entity.Account;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Rating;
import com.example.demo.entity.RatingId;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.exceptions.UnauthorizedException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.MovieRepository;
import com.example.demo.repository.RatingRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.RatingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RatingServiceImpl implements RatingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RatingServiceImpl.class);

  private final MovieRepository movieRepository;

  private final AccountRepository accountRepository;

  private final RatingRepository ratingRepository;

  public RatingServiceImpl(
      MovieRepository movieRepository,
      AccountRepository accountRepository,
      RatingRepository ratingRepository) {
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
    this.ratingRepository = ratingRepository;
  }

  @Override
  public Rating rateMovie(UserPrincipal currentAccount, Long movieId, BigDecimal score) {
    if (score.floatValue() < 0 || score.floatValue() > 10.1) {
      throw new BadRequestException("Score must be between 0 and 10");
    } else {
      Movie movie = movieRepository.getMovieById(movieId);
      Account account = accountRepository.getAccount(currentAccount);
      Rating rating =
          new Rating(score, movie, account, new RatingId(movie.getId(), account.getId()));
      Rating savedRating = ratingRepository.save(rating);
      LOGGER.info("rating with id [{}] was created.", savedRating.getId());
      return savedRating;
    }
  }

  @Override
  public List<Rating> getRatingsByAccount(UserPrincipal currentAccount) {
    Account account = accountRepository.getAccount(currentAccount);
    List<Rating> ratings = ratingRepository.findRatingsByAccount(account);
    if (ratings.isEmpty()) {
      throw new NotFoundException(
          "ratings of account with id [" + currentAccount.getId() + "] not found in database.");
    }
    LOGGER.info(
        "[{}] ratings from account with id [{}] were retrieved.",
        ratings.size(),
        currentAccount.getId());
    return ratings;
  }

  @Override
  public void deleteRating(UserPrincipal currentAccount, Long movieId) {
    Rating rating =
        ratingRepository
            .findRatingByAccountIdAndMovieId(currentAccount.getId(), movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Rating with movieId ["
                            + movieId
                            + "] and accountId ["
                            + currentAccount.getId()
                            + "] not found in database."));
    if (Objects.equals(rating.getAccount().getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      ratingRepository.delete(rating);
      LOGGER.info("rating with id [{}] was deleted.", rating.getId());
    } else {
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to delete this resource.");
    }
  }
}
