package app.popcornsociety.engagement.internal;

import app.popcornsociety.catalog.api.MovieRecord;
import app.popcornsociety.catalog.api.MovieReferenceService;
import app.popcornsociety.engagement.api.WatchlistCandidate;
import app.popcornsociety.engagement.api.WatchlistCandidateProvider;
import app.popcornsociety.engagement.internal.persistence.WatchedMovie;
import app.popcornsociety.engagement.internal.persistence.WatchedMovieRepository;
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
