package com.thecodinglab.imdbclone.engagement.internal.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, RatingId> {

  List<Rating> findRatingsByIdMovieId(Long movieId);

  Page<Rating> findRatingsByIdAccountId(Long accountId, Pageable pageable);

  List<Rating> findAllByIdAccountId(Long accountId);

  Optional<Rating> findByIdAccountIdAndIdMovieId(Long accountId, Long movieId);

  @org.springframework.data.jpa.repository.Query(
      "select r.id.movieId from Rating r where r.id.accountId = :accountId")
  java.util.Set<Long> findMovieIdsByAccountId(Long accountId);

  Long countByIdAccountId(Long accountId);
}
