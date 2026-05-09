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
});
