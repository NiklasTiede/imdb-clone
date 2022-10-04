package com.example.demo.service.impl;

import com.example.demo.entity.Account;
import com.example.demo.entity.Movie;
import com.example.demo.entity.WatchedMovie;
import com.example.demo.entity.WatchedMovieId;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.payload.MessageResponse;
import com.example.demo.payload.PagedResponse;
import com.example.demo.payload.WatchedMovieRecord;
import com.example.demo.payload.mapper.CustomWatchedMovieMapper;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.MovieRepository;
import com.example.demo.repository.WatchedMovieRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.WatchedMovieService;
import com.example.demo.util.Pagination;
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
    if (watchedMovies.getContent().isEmpty()) {
      throw new NotFoundException(
          "WatchedMovies of account with id [" + account.getId() + "] not found in database.");
    }
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
