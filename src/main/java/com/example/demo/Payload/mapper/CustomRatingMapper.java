package com.example.demo.Payload.mapper;

import com.example.demo.Payload.RatingRecord;
import com.example.demo.entity.Rating;
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
