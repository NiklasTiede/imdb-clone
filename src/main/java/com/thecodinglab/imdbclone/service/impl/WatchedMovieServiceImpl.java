package com.thecodinglab.imdbclone.service.impl;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.entity.WatchedMovie;
import com.thecodinglab.imdbclone.entity.WatchedMovieId;
import com.thecodinglab.imdbclone.exception.NotFoundException;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(WatchedMovieServiceImpl.class);

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
  public PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccount(
      String username, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Account account = accountRepository.getAccountByUsername(username);
    Page<WatchedMovie> watchedMovies =
        watchedMovieRepository.findAllByAccountIdOrderByCreatedAtInUtcDesc(
            account.getId(), pageable);
    Page<WatchedMovieRecord> watchedMovieRecordPage =
        watchedMovies.map(watchedMovieMapper::entityToDTO);
    LOGGER.info(
        "[{}] watchedMovies from account with id [{}] were retrieved.",
        watchedMovies.getContent().size(),
        account.getId());

    return new PagedResponse<>(
        watchedMovieRecordPage.getContent(),
        watchedMovieRecordPage.getNumber(),
        watchedMovieRecordPage.getSize(),
        watchedMovieRecordPage.getTotalElements(),
        watchedMovieRecordPage.getTotalPages(),
        watchedMovieRecordPage.isLast());
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
