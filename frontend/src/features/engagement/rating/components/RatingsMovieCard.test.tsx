import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import RatingsMovieCard from "./RatingsMovieCard";
import {
  imdbRatingBadgeSx,
  imdbRatingStarSx,
  yourRatingBadgeSx,
  yourRatingStarSx,
} from "./RatingsMovieCard.styles";

describe("RatingsMovieCard", () => {
  test("renders the user's score with an accent badge and theme star", () => {
    render(
      <MemoryRouter>
        <RatingsMovieCard
          item={{
            movie: {
              id: 7,
              imdbRating: 8.4,
              primaryTitle: "The Green Mile",
              startYear: 1999,
            },
            rating: 9,
          }}
        />
      </MemoryRouter>,
    );

    expect(screen.getByLabelText("Your rating 9 out of 10")).toBeTruthy();
    expect(screen.queryByText("The Green Mile")).toBeNull();
    expect(yourRatingStarSx.color).toBe("star");
    expect(imdbRatingStarSx.color).toBe("star");
    expect(typeof yourRatingBadgeSx.borderColor).toBe("function");
  });

  test("keeps user and IMDb rating badges the same size", () => {
    expect(yourRatingBadgeSx.fontSize).toBe(imdbRatingBadgeSx.fontSize);
    expect(yourRatingBadgeSx.fontWeight).toBe(imdbRatingBadgeSx.fontWeight);
    expect(yourRatingBadgeSx.px).toBe(imdbRatingBadgeSx.px);
    expect(yourRatingBadgeSx.py).toBe(imdbRatingBadgeSx.py);
    expect(yourRatingStarSx.fontSize).toBe(imdbRatingStarSx.fontSize);
  });
});
