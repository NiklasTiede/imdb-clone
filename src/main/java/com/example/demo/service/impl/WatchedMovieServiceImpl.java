package com.example.demo.service.impl;

import com.example.demo.Payload.MessageResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.Movie;
import com.example.demo.entity.WatchedMovie;
import com.example.demo.entity.WatchedMovieId;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.MovieRepository;
import com.example.demo.repository.WatchedMovieRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.WatchedMovieService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WatchedMovieServiceImpl implements WatchedMovieService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WatchedMovieServiceImpl.class);

  private final WatchedMovieRepository watchedMovieRepository;
  private final MovieRepository movieRepository;
  private final AccountRepository accountRepository;

  public WatchedMovieServiceImpl(
      WatchedMovieRepository watchedMovieRepository,
      MovieRepository movieRepository,
      AccountRepository accountRepository) {
    this.watchedMovieRepository = watchedMovieRepository;
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
  }

  @Override
  public WatchedMovie watchMovie(Long movieId, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    Account account = accountRepository.getAccount(currentAccount);
    WatchedMovie watchedMovie =
        new WatchedMovie(new WatchedMovieId(movie.getId(), account.getId()), movie, account);
    WatchedMovie savedWatchedMovie = watchedMovieRepository.save(watchedMovie);
    LOGGER.info(
        "Movie with id [{}] is watched by account with id [{}].",
        savedWatchedMovie.getId().getMovieId(),
        savedWatchedMovie.getId().getAccountId());
    return savedWatchedMovie;
  }

  @Override
  public List<WatchedMovie> getWatchedMoviesByAccount(UserPrincipal currentAccount) {
    List<WatchedMovie> watchedMovies =
        watchedMovieRepository.findAllByAccountIdOrderByCreatedAtInUtcDesc(currentAccount.getId());
    if (watchedMovies.isEmpty()) {
      throw new NotFoundException(
          "WatchedMovies of account with id ["
              + currentAccount.getId()
              + "] not found in database.");
    }
    LOGGER.info(
        "[{}] watchedMovies from account with id [{}] were retrieved.",
        watchedMovies.size(),
        currentAccount.getId());
    return watchedMovies;
  }

  @Override
  public MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount) {
    WatchedMovie watchedMovie =
        watchedMovieRepository
            .findWatchedMovieByMovieIdAndAccountId(movieId, currentAccount.getId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "WatchedMovie with movieId ["
                            + movieId
                            + "] and accountId ["
                            + currentAccount.getId()
                            + "] not found in database."));
    watchedMovieRepository.delete(watchedMovie);
    LOGGER.info(
        "WatchedMovie with movieId [{}] and accountId [{}] was deleted.",
        movieId,
        currentAccount.getId());
    return new MessageResponse(
        "WatchedMovie with movieId ["
            + movieId
            + "] and accountId ["
            + currentAccount.getId()
            + "] was deleted");
  }
}
