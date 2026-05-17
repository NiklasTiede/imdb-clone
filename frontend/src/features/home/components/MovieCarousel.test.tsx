import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import MovieCarousel from "./MovieCarousel";
import {
  movieCarouselCardWidthSx,
  movieCarouselScrollSx,
} from "./MovieCarousel.styles";

describe("MovieCarousel", () => {
  test("renders carousel headings, view-all action, and movie cards", () => {
    render(
      <MemoryRouter>
        <MovieCarousel
          title="Top horror"
          subtitle="Highest-rated horror from the last 10 years"
          movies={[
            {
              id: 1,
              primaryTitle: "Hereditary",
              startYear: 2018,
              runtimeMinutes: 127,
              imdbRating: 7.3,
            },
          ]}
          onViewAll={() => undefined}
        />
      </MemoryRouter>,
    );

    expect(screen.getByRole("heading", { name: "Top horror" })).toBeTruthy();
    expect(
      screen.getByText("Highest-rated horror from the last 10 years"),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: /view all/i })).toBeTruthy();
    expect(screen.getByRole("link", { name: /hereditary/i })).toBeTruthy();
  });

  test("uses fixed responsive card widths for stable horizontal scrolling", () => {
    expect(movieCarouselCardWidthSx).toEqual({ xs: 130, sm: 150, md: 170 });
  });

  test("keeps scroll clipping edges outside the aligned movie cards", () => {
    expect(movieCarouselScrollSx.mx).toEqual({ xs: 0, md: "-8px" });
    expect(movieCarouselScrollSx.px).toEqual({ xs: 2, md: "8px" });
  });

  test("shows six skeleton cards while loading", () => {
    render(
      <MovieCarousel
        title="Top horror"
        movies={[]}
        loading
      />,
    );

    expect(screen.getAllByTestId("movie-carousel-skeleton")).toHaveLength(6);
  });
});
