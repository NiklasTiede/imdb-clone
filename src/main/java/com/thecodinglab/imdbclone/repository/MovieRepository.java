package com.thecodinglab.imdbclone.repository;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

  /** Can be used in combination with indexMovies/partition-method to index movies */
  List<Movie> findByImdbRatingCountBetween(Integer minRatingCount, Integer maxRatingCount);

  Page<Movie> findByIdIn(List<Long> movieIds, Pageable pageable);

  default Movie getMovieById(Long movieId) {
    return findById(movieId)
        .orElseThrow(
            () -> new NotFoundException("Movie with id [" + movieId + "] not found in database."));
  }
}
