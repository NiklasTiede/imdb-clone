import { describe, expect, test } from "vitest";
import { watchlistQueries } from "./watchlistQueries";

describe("watchlistQueries", () => {
  test("builds a stable query key for the current user's watchlist", () => {
    expect(
      watchlistQueries.currentUserMovies({
        page: 0,
        size: 20,
        username: "test_user",
      }).queryKey,
    ).toEqual(["watchlist", "current-user", "test_user", 0, 20]);
  });

  test("does not run without a username", () => {
    expect(
      watchlistQueries.currentUserMovies({
        page: 0,
        size: 20,
        username: null,
      }).enabled,
    ).toBe(false);
  });

  test("builds a stable query key for the current user's watched movie ids", () => {
    expect(watchlistQueries.movieIds({ username: "ada" }).queryKey).toEqual([
      "watchlist",
      "current-user",
      "ada",
      "movie-ids",
    ]);
  });

  test("disables the movie-ids query without a username", () => {
    expect(watchlistQueries.movieIds({ username: null }).enabled).toBe(false);
    expect(watchlistQueries.movieIds({ username: "  " }).enabled).toBe(false);
  });
});
