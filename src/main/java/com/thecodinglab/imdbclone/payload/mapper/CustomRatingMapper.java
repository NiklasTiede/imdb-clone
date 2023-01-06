package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.entity.Rating;
import com.thecodinglab.imdbclone.payload.rating.RatingRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomRatingMapper {

  public RatingRecord entityToDTO(Rating rating) {
    return new RatingRecord(
        rating.getRating(), rating.getAccount().getId(), rating.getMovie().getId());
  }

  public List<RatingRecord> entityToDTO(List<Rating> ratings) {
    return ratings.stream().map(this::entityToDTO).toList();
  }
}
