package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.entity.WatchedMovie;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.mapper.CustomWatchedMovieMapper;
import com.thecodinglab.imdbclone.payload.watchlist.WatchedMovieRecord;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.repository.WatchedMovieRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.WatchedMovieService;
import com.thecodinglab.imdbclone.validation.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class WatchedMovieServiceImpl implements WatchedMovieService {

  private static final Logger logger = LoggerFactory.getLogger(WatchedMovieServiceImpl.class);

  private final WatchedMovieRepository watchedMovieRepository;
  private final MovieRepository movieRepository;
  private final AccountRepository accountRepository;
  private final CustomWatchedMovieMapper watchedMovieMapper;

  public WatchedMovieServiceImpl(
      WatchedMovieRepository watchedMovieRepository,
      MovieRepository movieRepository,
      AccountRepository accountRepository,
      CustomWatchedMovieMapper watchedMovieMapper) {
    this.watchedMovieRepository = watchedMovieRepository;
    this.movieRepository = movieRepository;
    this.accountRepository = accountRepository;
    this.watchedMovieMapper = watchedMovieMapper;
  }

  @Override
  public WatchedMovieRecord watchMovie(Long movieId, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    Account account = accountRepository.getAccount(currentAccount);
    WatchedMovie watchedMovie = WatchedMovie.create(movie, account);
    WatchedMovie savedWatchedMovie = watchedMovieRepository.save(watchedMovie);
    logger.info(
        "Movie with [{}] is watched by account with id [{}].",
        kv(WATCHED_MOVIE_ID, savedWatchedMovie.getId()),
        savedWatchedMovie.getId().getAccountId());
    return new WatchedMovieRecord(
        savedWatchedMovie.getAccount().getId(), savedWatchedMovie.getMovie().getId());
  }

  @Override
  public Page<WatchedMovieRecord> getWatchedMoviesByAccount(String username, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Account account = accountRepository.getAccountByUsername(username);
    Page<WatchedMovie> watchedMovies =
        watchedMovieRepository.findAllByAccountIdOrderByCreatedAtInUtcDesc(
            account.getId(), pageable);
    Page<WatchedMovieRecord> watchedMovieRecordPage =
        watchedMovies.map(watchedMovieMapper::entityToDTO);
    logger.info(
        "[{}] watchedMovies from account with [{}] were retrieved.",
        watchedMovies.getContent().size(),
        kv(ACCOUNT_ID, account.getId()));
    return watchedMovieRecordPage;
  }

  @Override
  public MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount) {
    WatchedMovie watchedMovie =
        watchedMovieRepository
            .findWatchedMovieByMovieIdAndAccountId(movieId, currentAccount.getId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "WatchedMovie with movieId [%d] and accountId [%d] not found in database."
                            .formatted(movieId, currentAccount.getId())));
    watchedMovieRepository.delete(watchedMovie);
    logger.info("WatchedMovie with [{}] was deleted.", kv(WATCHED_MOVIE_ID, watchedMovie.getId()));
    return new MessageResponse(
        "WatchedMovie with movieId [%d] and accountId [%d] was deleted"
            .formatted(movieId, currentAccount.getId()));
  }
}
