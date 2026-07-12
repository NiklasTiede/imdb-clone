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
  test("renders the user's score with a subdued badge style", () => {
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
    expect(yourRatingBadgeSx.backgroundColor).toBe("rgba(5,10,20,0.72)");
    expect(yourRatingBadgeSx.border).toBe("1px solid rgba(77,171,247,0.32)");
    expect(yourRatingStarSx.color).toBe("rgba(77,171,247,0.9)");
  });

  test("keeps user and IMDb rating badges the same size", () => {
    expect(yourRatingBadgeSx.fontSize).toBe(imdbRatingBadgeSx.fontSize);
    expect(yourRatingBadgeSx.fontWeight).toBe(imdbRatingBadgeSx.fontWeight);
    expect(yourRatingBadgeSx.px).toBe(imdbRatingBadgeSx.px);
    expect(yourRatingBadgeSx.py).toBe(imdbRatingBadgeSx.py);
    expect(yourRatingStarSx.fontSize).toBe(imdbRatingStarSx.fontSize);
  });
});
