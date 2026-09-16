package app.popcornsociety.engagement.api;

import app.popcornsociety.shared.api.PagedResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface AssistantWatchlist {
  PagedResponse<WatchedMovieRecord> read(Long accountId, int page);

  Receipt add(Long accountId, Long movieId, UUID operationId);

  AssistantActionReceipt remove(Long accountId, Long movieId, UUID operationId);

  record Receipt(UUID operationId, Long movieId, boolean created, Instant addedAt) {}
}
