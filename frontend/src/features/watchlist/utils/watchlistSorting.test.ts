import { describe, expect, test } from "vitest";
import { sortWatchlistItems } from "./watchlistSorting";

const items = [
  {
    addedAt: "2026-01-02T00:00:00Z",
    movieId: 1,
    movie: {
      primaryTitle: "Beta",
      imdbRating: 7,
      runtimeMinutes: 90,
    },
  },
  {
    addedAt: "2026-01-03T00:00:00Z",
    movieId: 2,
    movie: {
      primaryTitle: "Alpha",
      imdbRating: 8,
      runtimeMinutes: 120,
    },
  },
  {
    addedAt: "2026-01-01T00:00:00Z",
    movieId: 3,
    movie: {
      primaryTitle: "Gamma",
      imdbRating: 6,
      runtimeMinutes: 100,
    },
  },
];

describe("watchlistSorting", () => {
  test("sorts by newest addition by default", () => {
    expect(sortWatchlistItems(items, "addedAt_desc").map((item) => item.movieId))
      .toEqual([2, 1, 3]);
  });

  test("sorts by title, rating, and runtime", () => {
    expect(sortWatchlistItems(items, "title_asc").map((item) => item.movieId))
      .toEqual([2, 1, 3]);
    expect(sortWatchlistItems(items, "rating_asc").map((item) => item.movieId))
      .toEqual([3, 1, 2]);
    expect(sortWatchlistItems(items, "runtime_desc").map((item) => item.movieId))
      .toEqual([2, 3, 1]);
  });
});
