package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.engagement.api.RatingPreferenceProvider;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RatingPreferences implements RatingPreferenceProvider {
  private final RatingRepository ratings;
  private final WatchedMovieRepository watchlist;
  private final MovieReferenceService movies;

  RatingPreferences(
      RatingRepository ratings, WatchedMovieRepository watchlist, MovieReferenceService movies) {
    this.ratings = ratings;
    this.watchlist = watchlist;
    this.movies = movies;
  }

  @Override
  @Transactional(readOnly = true)
  public Preferences forAccount(Long accountId) {
    var best =
        ratings.findRatingsByIdAccountId(
            accountId,
            PageRequest.of(0, 3, Sort.by(Sort.Order.desc("rating"), Sort.Order.asc("id.movieId"))));
    var positive =
        best.stream().filter(r -> r.getRating().compareTo(BigDecimal.valueOf(7)) >= 0).toList();
    var catalog = movies.findMoviesByIds(positive.stream().map(r -> r.getMovieId()).toList());
    var favorites =
        positive.stream()
            .flatMap(
                r ->
                    catalog.stream()
                        .filter(m -> Objects.equals(m.id(), r.getMovieId()))
                        .map(m -> new Favorite(m.id(), m.primaryTitle(), r.getRating())))
            .toList();
    var excluded = new HashSet<>(ratings.findMovieIdsByAccountId(accountId));
    excluded.addAll(watchlist.findMovieIdsByAccountId(accountId));
    return new Preferences(favorites, excluded, best.getTotalElements());
  }
}
