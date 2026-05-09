import { describe, expect, test } from "vitest";
import { ratingQueries } from "./ratingQueries";

describe("ratingQueries", () => {
  test("builds a stable query key for the current user's rating of a movie", () => {
    expect(
      ratingQueries.userRatingForMovie({ movieId: 7, username: "ada" })
        .queryKey,
    ).toEqual(["rating", "current-user", "ada", "movie", 7]);
  });

  test("disables the query without a username or movie id", () => {
    expect(
      ratingQueries.userRatingForMovie({ movieId: 7, username: null }).enabled,
    ).toBe(false);
    expect(
      ratingQueries.userRatingForMovie({ movieId: null, username: "ada" })
        .enabled,
    ).toBe(false);
  });
});
