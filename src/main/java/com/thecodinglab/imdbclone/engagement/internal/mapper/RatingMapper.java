package com.thecodinglab.imdbclone.engagement.internal.mapper;

import com.thecodinglab.imdbclone.engagement.api.RatingRecord;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

  public RatingRecord entityToDTO(Rating rating) {
    return new RatingRecord(rating.getRating(), rating.getAccountId(), rating.getMovieId());
  }

  public List<RatingRecord> entityToDTO(List<Rating> ratings) {
    return ratings.stream().map(this::entityToDTO).toList();
  }
}
