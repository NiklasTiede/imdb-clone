package com.thecodinglab.imdbclone.engagement.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieService;
import com.thecodinglab.imdbclone.engagement.internal.mapper.WatchedMovieMapper;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovie;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Watchlist implements WatchedMovieService {

  private static final Logger logger = LoggerFactory.getLogger(Watchlist.class);

  private final WatchedMovieRepository watchedMovieRepository;
  private final MovieReferenceService movieReferenceService;
  private final WatchedMovieMapper watchedMovieMapper;

  public Watchlist(
      WatchedMovieRepository watchedMovieRepository,
      MovieReferenceService movieReferenceService,
      WatchedMovieMapper watchedMovieMapper) {
    this.watchedMovieRepository = watchedMovieRepository;
    this.movieReferenceService = movieReferenceService;
    this.watchedMovieMapper = watchedMovieMapper;
  }

  @Override
  @Transactional
  public WatchedMovieRecord watchMovie(Long movieId, UserPrincipal currentAccount) {
    MovieRecord movie = movieReferenceService.findMovieById(movieId);
    WatchedMovie watchedMovie = WatchedMovie.create(movieId, currentAccount.getId());
    WatchedMovie savedWatchedMovie = watchedMovieRepository.saveAndFlush(watchedMovie);
    logger.info(
        "Movie with [{}] is watched by account with id [{}].",
        kv(WATCHED_MOVIE_ID, savedWatchedMovie.getId()),
        savedWatchedMovie.getId().getAccountId());
    return watchedMovieMapper.entityToDTO(savedWatchedMovie, movie);
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
    Map<Long, MovieRecord> moviesById =
        movieReferenceService
            .findMoviesByIds(
                watchedMovies.getContent().stream().map(WatchedMovie::getMovieId).toList())
            .stream()
            .collect(java.util.stream.Collectors.toMap(MovieRecord::id, Function.identity()));
    watchedMovies.getContent().stream()
        .filter(watchedMovie -> !moviesById.containsKey(watchedMovie.getMovieId()))
        .forEach(
            watchedMovie ->
                logger.warn(
                    "Watched movie references a catalog movie that no longer exists [{}].",
                    kv(MOVIE_ID, watchedMovie.getMovieId())));
    List<WatchedMovieRecord> content =
        watchedMovieMapper.entityToDTO(watchedMovies.getContent(), moviesById);
    long totalElements =
        Math.max(
            0,
            watchedMovies.getTotalElements() - watchedMovies.getContent().size() + content.size());
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PagedResponse<>(
        content, page, size, totalElements, totalPages, watchedMovies.isLast());
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
