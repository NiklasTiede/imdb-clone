package com.thecodinglab.imdbclone.engagement.internal.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {

  List<Rating> findRatingsByMovieId(Long movieId);

  Page<Rating> findRatingsByAccountId(Long accountId, Pageable pageable);

  Optional<Rating> findRatingByAccountIdAndMovieId(Long accountId, Long movieId);

  List<Rating> findAllByModifiedAtInUtcAfter(Instant modifiedAt);

  Long countRatingsByAccountId(Long accountId);
}
