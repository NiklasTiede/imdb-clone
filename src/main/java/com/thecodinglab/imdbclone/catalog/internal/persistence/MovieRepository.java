package com.thecodinglab.imdbclone.catalog.internal.persistence;

import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {

  Page<Movie> findByIdIn(List<Long> movieIds, Pageable pageable);

  List<Movie> findByIdIn(Collection<Long> movieIds);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          update movie
          set rating = case
                  when rating_count + :ratingCountDelta = 0 then null
                  else round((rating_sum + :ratingSumDelta) / (rating_count + :ratingCountDelta), 1)
              end,
              rating_sum = rating_sum + :ratingSumDelta,
              rating_count = rating_count + :ratingCountDelta
          where id = :movieId
            and rating_count + :ratingCountDelta >= 0
          """,
      nativeQuery = true)
  int applyRatingAggregateDelta(
      @Param("movieId") Long movieId,
      @Param("ratingSumDelta") BigDecimal ratingSumDelta,
      @Param("ratingCountDelta") int ratingCountDelta);

  default Movie getMovieById(Long movieId) {
    return findById(movieId)
        .orElseThrow(
            () -> new NotFoundException("Movie with id [" + movieId + "] not found in database."));
  }
}
