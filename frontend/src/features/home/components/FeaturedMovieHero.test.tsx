import { render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import FeaturedMovieHero from "./FeaturedMovieHero";

describe("FeaturedMovieHero", () => {
  test("shows an explicit fallback when the featured movie cannot load", () => {
    render(
      <FeaturedMovieHero
        error
        movies={[]}
        onToggleBookmark={vi.fn()}
        onViewMovie={vi.fn()}
      />,
    );

    expect(screen.getByText("Featured picks unavailable")).toBeTruthy();
    expect(
      screen.getByText("The homepage is still ready to explore below."),
    ).toBeTruthy();
  });

  test("renders one lead and two supporting featured movies", () => {
    render(
      <FeaturedMovieHero
        movies={[
          movie(1, "The Lead", 8.8),
          movie(2, "Second Pick", 8.2),
          movie(3, "Third Pick", 7.9),
        ]}
        onToggleBookmark={vi.fn()}
        onViewMovie={vi.fn()}
      />,
    );

    expect(screen.getAllByTestId("featured-movie-card")).toHaveLength(3);
    expect(screen.getByText("The Lead")).toBeTruthy();
    expect(screen.getByText("Second Pick")).toBeTruthy();
    expect(screen.getByText("Third Pick")).toBeTruthy();
  });
});

const movie = (id: number, primaryTitle: string, imdbRating: number) => ({
  id,
  primaryTitle,
  imdbRating,
  imdbRatingCount: 150_000,
  startYear: 2020,
  runtimeMinutes: 120,
});
