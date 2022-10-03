package com.example.demo.payload.mapper;

import com.example.demo.entity.Rating;
import com.example.demo.payload.RatingRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomRatingMapper {

  public RatingRecord entityToDTO(Rating rating) {
    return new RatingRecord(
        rating.getRating(), rating.getAccount().getId(), rating.getMovie().getId());
  }

  public List<RatingRecord> entityToDTO(List<Rating> comments) {
    return comments.stream().map(this::entityToDTO).toList();
  }
}
