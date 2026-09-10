package com.thecodinglab.imdbclone.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thecodinglab.imdbclone.engagement.api.*;
import com.thecodinglab.imdbclone.identity.api.ConciergeDelegation;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.security.access.AccessDeniedException;

class PersonalMcpToolsTest {
  private final ConciergeDelegation delegation = mock(ConciergeDelegation.class);
  private final AssistantWatchlist watchlist = mock(AssistantWatchlist.class);
  private final AssistantRatings ratings = mock(AssistantRatings.class);
  private final PersonalToolAuthorization authorization = new PersonalToolAuthorization(delegation);
  private final WatchlistMcpTools watchlistTools = new WatchlistMcpTools(authorization, watchlist);
  private final RatingMcpTools ratingTools = new RatingMcpTools(authorization, ratings);
  private final McpMeta meta = mock(McpMeta.class);

  private void authorize(String scope) {
    when(meta.get("delegation")).thenReturn("synthetic-delegation");
    when(delegation.verify("synthetic-delegation", scope))
        .thenReturn(new ConciergeDelegation.Actor(7L, "session-binding"));
  }

  @Test
  void contextVerifiesTheSessionWithoutReadingPersonalData() {
    authorize("watchlist:read");
    assertThat(watchlistTools.getMyContext(meta))
        .isEqualTo(new WatchlistMcpTools.Context("session-binding", true));
    verify(delegation).verify("synthetic-delegation", "watchlist:read");
    verifyNoInteractions(watchlist, ratings);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 100})
  void readsTheDelegatedAccountsPageAndPreservesCatalogAndPagination(int page) {
    authorize("watchlist:read");
    var movie = McpProtocolContractTest.CapturingMovieSearch.movie();
    when(watchlist.read(7L, page))
        .thenReturn(
            new PagedResponse<>(
                List.of(new WatchedMovieRecord(7L, movie.id(), Instant.EPOCH, movie)),
                page,
                20,
                2001,
                101,
                page == 100));
    var result = watchlistTools.getMyWatchlist(meta, page);
    assertThat(result.contractVersion()).isEqualTo("1.0");
    assertThat(result.movies()).containsExactly(MovieToolMovie.from(movie));
    assertThat(result.page()).isEqualTo(page);
    assertThat(result.totalElements()).isEqualTo(2001);
    assertThat(result.last()).isEqualTo(page == 100);
    verify(watchlist).read(7L, page);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 101})
  void invalidPagesCannotReachTheDomain(int page) {
    authorize("watchlist:read");
    assertThatThrownBy(() -> watchlistTools.getMyWatchlist(meta, page))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid watchlist page");
    verifyNoInteractions(watchlist);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void removalReturnsTheCommittedChangeAndPriorRating(boolean changed) {
    var operation = UUID.randomUUID();
    when(meta.get("operationId")).thenReturn(operation.toString());
    authorize("watchlist:remove");
    var watchlistReceipt =
        new AssistantActionReceipt(
            operation, 42L, "watchlist_remove", changed, null, null, Instant.EPOCH);
    when(watchlist.remove(7L, 42L, operation)).thenReturn(watchlistReceipt);
    assertThat(watchlistTools.removeMovieFromMyWatchlist(meta, 42L))
        .isEqualTo(PersonalActionResult.from(watchlistReceipt));
    verify(watchlist).remove(7L, 42L, operation);
    authorize("ratings:remove");
    var ratingReceipt =
        new AssistantActionReceipt(
            operation,
            42L,
            "rating_remove",
            changed,
            null,
            changed ? new BigDecimal("8.5") : null,
            Instant.EPOCH);
    when(ratings.remove(7L, 42L, operation)).thenReturn(ratingReceipt);
    assertThat(ratingTools.removeMyMovieRating(meta, 42L))
        .isEqualTo(PersonalActionResult.from(ratingReceipt));
    verify(ratings).remove(7L, 42L, operation);
  }

  @Test
  void rejectedDelegationCannotReadOrChangeAnyPersonalData() {
    when(delegation.verify(null, "watchlist:read")).thenThrow(new AccessDeniedException("Denied"));
    when(delegation.verify(null, "ratings:remove")).thenThrow(new AccessDeniedException("Denied"));
    assertThatThrownBy(() -> watchlistTools.getMyContext(meta))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> watchlistTools.getMyWatchlist(meta, 0))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> ratingTools.removeMyMovieRating(meta, 42L))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(watchlist, ratings);
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 0})
  void invalidMovieIdsNeverReachTheDomain(long movieId) {
    authorize("watchlist:add");
    assertThatThrownBy(() -> watchlistTools.addMovieToMyWatchlist(meta, movieId))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(watchlist);
  }

  @Test
  void missingMovieOrOperationMetadataCannotMutatePersonalData() {
    authorize("watchlist:add");
    assertThatThrownBy(() -> watchlistTools.addMovieToMyWatchlist(meta, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid movie");
    for (Object value : new Object[] {null, 123, "invalid-operation"}) {
      when(meta.get("operationId")).thenReturn(value);
      assertThatThrownBy(() -> watchlistTools.addMovieToMyWatchlist(meta, 42L))
          .isInstanceOf(IllegalArgumentException.class);
    }
    verifyNoInteractions(watchlist);
  }

  @Test
  void nonStringDelegationIsTreatedAsMissing() {
    when(meta.get("delegation")).thenReturn(123);
    when(delegation.verify(null, "watchlist:read")).thenThrow(new AccessDeniedException("Denied"));
    assertThatThrownBy(() -> watchlistTools.getMyWatchlist(meta, 0))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(watchlist);
  }
}
