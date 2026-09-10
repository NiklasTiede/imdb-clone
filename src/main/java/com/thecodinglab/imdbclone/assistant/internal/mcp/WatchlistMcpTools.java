package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.engagement.api.AssistantWatchlist;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class WatchlistMcpTools {
  private final PersonalToolAuthorization authorization;
  private final AssistantWatchlist watchlist;

  public WatchlistMcpTools(PersonalToolAuthorization authorization, AssistantWatchlist watchlist) {
    this.authorization = authorization;
    this.watchlist = watchlist;
  }

  public record Context(String sessionBinding, boolean authenticated) {}

  public record WatchlistResult(
      String contractVersion,
      List<MovieToolMovie> movies,
      int page,
      long totalElements,
      boolean last) {}

  public record AddResult(
      String contractVersion, String operationId, long movieId, boolean created, String addedAt) {}

  @McpTool(
      name = "get_my_context",
      description = "Verify the delegated session. Application-only capability; not a model tool.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public Context getMyContext(McpMeta meta) {
    var actor = authorization.actor(meta, "watchlist:read");
    return new Context(actor.sessionBinding(), true);
  }

  @McpTool(
      name = "get_my_watchlist",
      description =
          "Read the signed-in user's actual watchlist, 20 movies per page. Never accepts an account ID.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public WatchlistResult getMyWatchlist(
      McpMeta meta,
      @McpToolParam(required = true, description = "Zero-based page number, 0 to 100.") int page) {
    var actor = authorization.actor(meta, "watchlist:read");
    if (page < 0 || page > 100) throw new IllegalArgumentException("Invalid watchlist page");
    var result = watchlist.read(actor.accountId(), page);
    return new WatchlistResult(
        "1.0",
        result.getContent().stream().map(m -> MovieToolMovie.from(m.movie())).toList(),
        page,
        result.getTotalElements(),
        result.isLast());
  }

  @McpTool(
      name = "add_movie_to_my_watchlist",
      description =
          "Add one grounded movie to the signed-in user's watchlist only after their explicit add command. Returns the committed receipt; never claim success before it returns.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = false,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public AddResult addMovieToMyWatchlist(
      McpMeta meta,
      @McpToolParam(
              required = true,
              description =
                  "Positive movie ID from a catalog result matching the explicit user command.")
          Long movieId) {
    var actor = authorization.actor(meta, "watchlist:add");
    var receipt =
        watchlist.add(
            actor.accountId(),
            PersonalToolAuthorization.validatedMovie(movieId),
            PersonalToolAuthorization.operation(meta));
    return new AddResult(
        "1.0",
        receipt.operationId().toString(),
        movieId,
        receipt.created(),
        receipt.addedAt().toString());
  }

  @McpTool(
      name = "remove_movie_from_my_watchlist",
      description =
          "Remove one grounded movie from the signed-in user's watchlist after an explicit remove command. A successful receipt says whether an entry was removed or was already absent.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = false,
              destructiveHint = true,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public PersonalActionResult removeMovieFromMyWatchlist(
      McpMeta meta,
      @McpToolParam(
              required = true,
              description = "Positive catalog-grounded movie ID from the explicit command.")
          Long movieId) {
    var actor = authorization.actor(meta, "watchlist:remove");
    return PersonalActionResult.from(
        watchlist.remove(
            actor.accountId(),
            PersonalToolAuthorization.validatedMovie(movieId),
            PersonalToolAuthorization.operation(meta)));
  }
}
