package com.thecodinglab.imdbclone.engagement.internal.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {

  List<Rating> findRatingsByIdMovieId(Long movieId);

  Page<Rating> findRatingsByIdAccountId(Long accountId, Pageable pageable);

  Optional<Rating> findByIdAccountIdAndIdMovieId(Long accountId, Long movieId);

  Long countByIdAccountId(Long accountId);
}
