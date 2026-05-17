import { describe, expect, test } from "vitest";
import {
  SEARCH_RESULTS_MAX_WIDTH_PX,
  SEARCH_VIEW_STORAGE_KEY,
  shouldShowSearchEmptyState,
  sortSearchMovies,
} from "./MovieSearchPage.utils";

describe("MovieSearchPage layout", () => {
  test("is wide enough for six desktop search cards", () => {
    const cardWidth = 190;
    const gapWidth = 16;
    const cardsPerRow = 6;

    expect(SEARCH_RESULTS_MAX_WIDTH_PX).toBeGreaterThanOrEqual(
      cardsPerRow * cardWidth + (cardsPerRow - 1) * gapWidth,
    );
  });

  test("uses a stable local storage key for the preferred results view", () => {
    expect(SEARCH_VIEW_STORAGE_KEY).toBe("search.view");
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

  test("does not show the empty state while an error is visible", () => {
    expect(
      shouldShowSearchEmptyState({
        hasSearchCriteria: true,
        isError: true,
        isFetching: false,
        movieCount: 0,
      }),
    ).toBe(false);
  });

  test("shows the empty state for a successful empty search", () => {
    expect(
      shouldShowSearchEmptyState({
        hasSearchCriteria: true,
        isError: false,
        isFetching: false,
        movieCount: 0,
      }),
    ).toBe(true);
  });
});
