package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.engagement.api.WatchlistCandidate;
import com.thecodinglab.imdbclone.engagement.api.WatchlistCandidateProvider;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovie;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class WatchlistCandidates implements WatchlistCandidateProvider {

  private final WatchedMovieRepository watchedMovieRepository;
  private final MovieReferenceService movieReferenceService;

  WatchlistCandidates(
      WatchedMovieRepository watchedMovieRepository, MovieReferenceService movieReferenceService) {
    this.watchedMovieRepository = watchedMovieRepository;
    this.movieReferenceService = movieReferenceService;
  }

  @Override
  public java.util.List<WatchlistCandidate> findCandidates(Long accountId) {
    java.util.List<WatchedMovie> watchedMovies =
        watchedMovieRepository.findAllByIdAccountId(accountId);
    Map<Long, MovieRecord> moviesById =
        movieReferenceService
            .findMoviesByIds(watchedMovies.stream().map(WatchedMovie::getMovieId).toList())
            .stream()
            .filter(movie -> movie.id() != null)
            .collect(Collectors.toMap(MovieRecord::id, Function.identity()));
    return watchedMovies.stream()
        .flatMap(
            watchedMovie -> {
              MovieRecord movie = moviesById.get(watchedMovie.getMovieId());
              return movie == null
                  ? java.util.stream.Stream.empty()
                  : java.util.stream.Stream.of(
                      new WatchlistCandidate(movie, watchedMovie.getCreatedAtInUtc()));
            })
        .toList();
  }
}
