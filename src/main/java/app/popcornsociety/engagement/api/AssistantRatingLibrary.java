package app.popcornsociety.engagement.api;

import app.popcornsociety.catalog.api.MovieRecord;
import app.popcornsociety.shared.api.PagedResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface AssistantRatingLibrary {
  enum Order {
    HIGHEST,
    LOWEST,
    RECENT
  }

  record Entry(MovieRecord movie, BigDecimal userScore, Instant ratedAt) {}

  record Facet(String label, int movieCount, BigDecimal averageUserScore) {}

  record Result(
      PagedResponse<Entry> items,
      BigDecimal averageUserScore,
      List<Facet> favoriteGenres,
      List<Facet> favoriteDecades) {}

  Result read(Long accountId, int page, Order order);
}
