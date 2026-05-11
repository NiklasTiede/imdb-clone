import { describe, expect, test } from "vitest";
import {
  SEARCH_RESULTS_MAX_WIDTH_PX,
  sortSearchMovies,
} from "./MovieSearchPage";

describe("MovieSearchPage layout", () => {
  test("is wide enough for six desktop search cards", () => {
    const cardWidth = 190;
    const gapWidth = 16;
    const cardsPerRow = 6;

    expect(SEARCH_RESULTS_MAX_WIDTH_PX).toBeGreaterThanOrEqual(
      cardsPerRow * cardWidth + (cardsPerRow - 1) * gapWidth,
    );
  });

  test("sorts filter-only result rows by IMDb rating descending", () => {
    expect(
      sortSearchMovies(
        [
          { id: 1, primaryTitle: "Low", imdbRating: 6.1 },
          { id: 2, primaryTitle: "High", imdbRating: 8.3 },
          { id: 3, primaryTitle: "Missing" },
        ],
        "rating_desc",
      ).map((movie) => movie.primaryTitle),
    ).toEqual(["High", "Low", "Missing"]);
  });
});
