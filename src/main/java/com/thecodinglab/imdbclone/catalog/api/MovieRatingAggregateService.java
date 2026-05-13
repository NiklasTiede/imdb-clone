package com.thecodinglab.imdbclone.catalog.api;

import java.math.BigDecimal;
import org.springframework.modulith.NamedInterface;

@NamedInterface("ratings")
public interface MovieRatingAggregateService {

  MovieRecord updateRatingAggregate(Long movieId, BigDecimal rating, int ratingCount);
}
