package com.thecodinglab.imdbclone.catalog.api;

import java.math.BigDecimal;
import org.springframework.modulith.NamedInterface;

@NamedInterface("ratings")
public interface MovieRatingAggregateService {

  void applyRatingAggregateDelta(Long movieId, BigDecimal ratingSumDelta, int ratingCountDelta);
}
