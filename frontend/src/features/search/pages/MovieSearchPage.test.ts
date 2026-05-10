import { describe, expect, test } from "vitest";
import { SEARCH_RESULTS_MAX_WIDTH_PX } from "./MovieSearchPage";

describe("MovieSearchPage layout", () => {
  test("is wide enough for six desktop search cards", () => {
    const cardWidth = 190;
    const gapWidth = 16;
    const cardsPerRow = 6;

    expect(SEARCH_RESULTS_MAX_WIDTH_PX).toBeGreaterThanOrEqual(
      cardsPerRow * cardWidth + (cardsPerRow - 1) * gapWidth,
    );
  });
});
