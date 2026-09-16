package app.popcornsociety.engagement.internal.mapper;

import app.popcornsociety.engagement.api.RatingRecord;
import app.popcornsociety.engagement.internal.persistence.Rating;
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
