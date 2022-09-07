package com.example.demo.service.impl;

import com.example.demo.entity.Account;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Rating;
import com.example.demo.entity.RatingId;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.MovieRepository;
import com.example.demo.repository.RatingRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.RatingService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RatingServiceImpl implements RatingService {

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
  public Rating rateMovie(Long movieId, BigDecimal score, UserPrincipal currentUser) {
    Movie movie =
        movieRepository
            .findById(movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Movie with MovieId [" + movieId + "] not found in database."));
    Account account = accountRepository.getAccount(currentUser);
    Rating rating = new Rating(score, movie, account, new RatingId(movie.getId(), account.getId()));
    return ratingRepository.save(rating);
  }

  @Override
  public List<Rating> getRatingsByAccount(UserPrincipal currentUser) {
    Account account = accountRepository.getAccount(currentUser);
    return ratingRepository.findRatingsByAccount(account);
  }
}
