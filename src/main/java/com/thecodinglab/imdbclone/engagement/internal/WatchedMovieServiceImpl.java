package com.thecodinglab.imdbclone.engagement.internal;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieService;
import com.thecodinglab.imdbclone.engagement.internal.mapper.WatchedMovieMapper;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovie;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.validation.Pagination;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchedMovieServiceImpl implements WatchedMovieService {

  private static final Logger logger = LoggerFactory.getLogger(WatchedMovieServiceImpl.class);

  private final WatchedMovieRepository watchedMovieRepository;
  private final MovieService movieService;
  private final EntityManager entityManager;
  private final WatchedMovieMapper watchedMovieMapper;

  public WatchedMovieServiceImpl(
      WatchedMovieRepository watchedMovieRepository,
      MovieService movieService,
      EntityManager entityManager,
      WatchedMovieMapper watchedMovieMapper) {
    this.watchedMovieRepository = watchedMovieRepository;
    this.movieService = movieService;
    this.entityManager = entityManager;
    this.watchedMovieMapper = watchedMovieMapper;
  }

  @Override
  @Transactional
  public WatchedMovieRecord watchMovie(Long movieId, UserPrincipal currentAccount) {
    movieService.findMovieById(movieId);
    Movie movie = entityManager.getReference(Movie.class, movieId);
    WatchedMovie watchedMovie = WatchedMovie.create(movie, currentAccount.getId());
    WatchedMovie savedWatchedMovie = watchedMovieRepository.saveAndFlush(watchedMovie);
    logger.info(
        "Movie with [{}] is watched by account with id [{}].",
        kv(WATCHED_MOVIE_ID, savedWatchedMovie.getId()),
        savedWatchedMovie.getId().getAccountId());
    return watchedMovieMapper.entityToDTO(savedWatchedMovie);
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResponse<WatchedMovieRecord> getWatchedMoviesByAccountId(
      Long accountId, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAtInUtc").descending());
    Page<WatchedMovie> watchedMovies =
        watchedMovieRepository.findAllByIdAccountIdOrderByCreatedAtInUtcDesc(accountId, pageable);
    logger.info(
        "[{}] watchedMovies from account with [{}] were retrieved.",
        watchedMovies.getContent().size(),
        kv(ACCOUNT_ID, accountId));
    return PagedResponse.from(watchedMovies.map(watchedMovieMapper::entityToDTO));
  }

  @Override
  @Transactional
  public MessageResponse deleteWatchedMovie(Long movieId, UserPrincipal currentAccount) {
    WatchedMovie watchedMovie =
        watchedMovieRepository
            .findByIdMovieIdAndIdAccountId(movieId, currentAccount.getId())
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
