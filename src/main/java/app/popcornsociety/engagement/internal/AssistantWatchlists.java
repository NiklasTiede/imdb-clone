package app.popcornsociety.engagement.internal;

import app.popcornsociety.engagement.api.AssistantActionReceipt;
import app.popcornsociety.engagement.api.AssistantWatchlist;
import app.popcornsociety.engagement.api.WatchedMovieRecord;
import app.popcornsociety.shared.api.PagedResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantWatchlists implements AssistantWatchlist {
  private final Watchlist watchlist;
  private final AssistantActionReceipts receipts;

  public AssistantWatchlists(Watchlist watchlist, AssistantActionReceipts receipts) {
    this.watchlist = watchlist;
    this.receipts = receipts;
  }

  @Override
  public PagedResponse<WatchedMovieRecord> read(Long accountId, int page) {
    return watchlist.getWatchedMoviesByAccountId(accountId, page, 20);
  }

  @Override
  @Transactional
  public Receipt add(Long accountId, Long movieId, UUID operationId) {
    var receipt =
        receipts.execute(
            accountId,
            movieId,
            operationId,
            "watchlist_add",
            null,
            () -> {
              var result = watchlist.addForAccount(movieId, accountId);
              return new AssistantActionReceipt(
                  operationId,
                  movieId,
                  "watchlist_add",
                  result.created(),
                  null,
                  null,
                  result.resource().addedAt());
            });
    return new Receipt(operationId, movieId, receipt.changed(), receipt.occurredAt());
  }

  @Override
  @Transactional
  public AssistantActionReceipt remove(Long accountId, Long movieId, UUID operationId) {
    return receipts.execute(
        accountId,
        movieId,
        operationId,
        "watchlist_remove",
        null,
        () ->
            new AssistantActionReceipt(
                operationId,
                movieId,
                "watchlist_remove",
                watchlist.removeForAccount(movieId, accountId),
                null,
                null,
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
  }
}
