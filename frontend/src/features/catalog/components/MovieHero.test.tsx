import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";
import { MemoryRouter } from "react-router";
import {
  MovieRecord,
  MovieRecordMovieGenreEnum,
  MovieRecordMovieTypeEnum,
} from "../../../client/movies/generator-output";
import { MovieHero } from "./MovieHero";

const renderHero = (overrides: Partial<Parameters<typeof MovieHero>[0]>) => {
  const defaultMovie: MovieRecord = {
    id: 1,
    primaryTitle: "It Follows",
    originalTitle: "It Follows",
    startYear: 2014,
    runtimeMinutes: 100,
    movieType: MovieRecordMovieTypeEnum.Movie,
    movieGenre: new Set([
      MovieRecordMovieGenreEnum.Horror,
      MovieRecordMovieGenreEnum.Mystery,
    ]),
    imdbRating: 6.8,
    imdbRatingCount: 245000,
    rating: 7.2,
    ratingCount: 1200,
    description: "A curse passed via intercourse.",
    imageUrlToken: undefined,
  };

  return render(
    <MemoryRouter>
      <MovieHero
        movie={defaultMovie}
        isBookmarked={false}
        onToggleBookmark={vi.fn()}
        isBookmarkLoading={false}
        userRating={null}
        onRate={vi.fn()}
        {...overrides}
      />
    </MemoryRouter>,
  );
};

describe("MovieHero", () => {
  test("renders title, meta row, genre chips, and both ratings", () => {
    renderHero({});

    expect(screen.getByRole("heading", { name: "It Follows" })).toBeTruthy();
    expect(screen.getByText("2014")).toBeTruthy();
    expect(screen.getByText("Movie")).toBeTruthy();
    expect(screen.getByText("100 min")).toBeTruthy();
    expect(screen.getByText("Horror")).toBeTruthy();
    expect(screen.getByText("Mystery")).toBeTruthy();
    expect(screen.getByText("IMDb rating")).toBeTruthy();
    expect(screen.getByText("Community")).toBeTruthy();
    expect(screen.getByText("6.8")).toBeTruthy();
    expect(screen.getByText("7.2")).toBeTruthy();
    expect(screen.getByText("/ 10 · 245k")).toBeTruthy();
    expect(screen.getByText("/ 10 · 1.2k")).toBeTruthy();
  });

  test("hides original title when it matches the primary title", () => {
    renderHero({});
    expect(screen.queryByText(/^Original:/)).toBeNull();
  });

  test("shows original title when it differs from primary", () => {
    renderHero({
      movie: {
        primaryTitle: "Spirited Away",
        originalTitle: "千と千尋の神隠し",
        startYear: 2001,
        runtimeMinutes: 125,
        movieType: MovieRecordMovieTypeEnum.Movie,
      },
    });
    expect(screen.getByText("Original: 千と千尋の神隠し")).toBeTruthy();
  });

  test("shows endYear range for series", () => {
    renderHero({
      movie: {
        primaryTitle: "Breaking Bad",
        originalTitle: "Breaking Bad",
        startYear: 2008,
        endYear: 2013,
        movieType: MovieRecordMovieTypeEnum.TvSeries,
        runtimeMinutes: 49,
      },
    });
    expect(screen.getByText("2008 – 2013")).toBeTruthy();
    expect(screen.getByText("Tv Series")).toBeTruthy();
  });

  test("bookmark button reflects bookmarked state and calls handler on click", async () => {
    const user = userEvent.setup();
    const onToggleBookmark = vi.fn();

    const { rerender } = renderHero({ onToggleBookmark });

    const button = screen.getByRole("button", { name: /add to watchlist/i });
    await user.click(button);
    expect(onToggleBookmark).toHaveBeenCalledTimes(1);

    rerender(
      <MemoryRouter>
        <MovieHero
          movie={{
            id: 1,
            primaryTitle: "It Follows",
            originalTitle: "It Follows",
            startYear: 2014,
            runtimeMinutes: 100,
            movieType: MovieRecordMovieTypeEnum.Movie,
            movieGenre: new Set([MovieRecordMovieGenreEnum.Horror]),
            imdbRating: 6.8,
            imdbRatingCount: 245000,
            rating: 7.2,
            ratingCount: 1200,
          }}
          isBookmarked={true}
          onToggleBookmark={onToggleBookmark}
          isBookmarkLoading={false}
          userRating={null}
          onRate={vi.fn()}
        />
      </MemoryRouter>,
    );
    expect(
      screen.getByRole("button", { name: /in your watchlist/i }),
    ).toBeTruthy();
  });

  test("rates the movie 1-10 when a star is clicked", async () => {
    const user = userEvent.setup();
    const onRate = vi.fn();
    renderHero({ onRate });

    const ratingGroup = screen.getByTestId("user-rating-stars");
    const stars = within(ratingGroup).getAllByRole("radio");
    expect(stars).toHaveLength(11);

    await user.click(stars[6]);
    expect(onRate).toHaveBeenCalledWith(7);
  });
});
