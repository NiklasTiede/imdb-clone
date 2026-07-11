import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";
import {
  MovieRecordMovieGenreEnum,
  MovieRecordMovieTypeEnum,
} from "../../../client/movies/generator-output";
import type { Movie } from "../model/movie";
import { MovieHero } from "./MovieHero";

const defaultMovie: Movie = {
  id: 1,
  imdbId: "tt0111161",
  tmdbId: 278,
  primaryTitle: "The Shawshank Redemption",
  originalTitle: "The Shawshank Redemption",
  startYear: 1994,
  runtimeMinutes: 142,
  movieType: MovieRecordMovieTypeEnum.Movie,
  movieGenre: new Set([MovieRecordMovieGenreEnum.Drama]),
  imdbRating: 9.3,
  imdbRatingCount: 3189003,
  rating: 8.8,
  ratingCount: 1240,
  description: "Two imprisoned men bond over a number of years.",
  posterImageToken: "poster-token",
  backdropImageToken: "backdrop-token",
};

const renderHero = (
  overrides: Partial<Parameters<typeof MovieHero>[0]> = {},
) => {
  const props = {
    movie: defaultMovie,
    isBookmarked: false,
    onToggleBookmark: vi.fn(),
    userRating: null,
    onOpenRating: vi.fn(),
    onShare: vi.fn(),
    ...overrides,
  };

  render(<MovieHero {...props} />);
  return props;
};

describe("MovieHero", () => {
  test("renders backdrop, poster, identity, metadata, genres, and ratings", () => {
    renderHero();

    expect(
      screen.getByRole("heading", { name: "The Shawshank Redemption" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("img", {
        name: "The Shawshank Redemption poster",
      }),
    ).toBeTruthy();
    expect(
      screen.getByRole("presentation", { hidden: true }).getAttribute("src"),
    ).toContain("backdrop-token_size_1280x720.webp");
    expect(screen.getByText("1994")).toBeTruthy();
    expect(screen.getByText("Movie")).toBeTruthy();
    expect(screen.getByText("2h 22m")).toBeTruthy();
    expect(screen.getByText("Drama")).toBeTruthy();
    expect(screen.getByText("IMDb rating")).toBeTruthy();
    expect(screen.getByText("Community")).toBeTruthy();
    expect(screen.getByText("9.3")).toBeTruthy();
    expect(screen.getByText("8.8")).toBeTruthy();
    expect(screen.queryByRole("link", { name: /View on/ })).toBeNull();
  });

  test("renders a conditional original title", () => {
    renderHero({
      movie: {
        ...defaultMovie,
        originalTitle: "Die Verurteilten",
      },
    });

    expect(screen.getByText("Original title: Die Verurteilten")).toBeTruthy();
  });

  test("renders a trailer preview only for a valid stored key", () => {
    renderHero({
      movie: { ...defaultMovie, trailerYoutubeKey: "abcDEF123_-" },
    });

    expect(screen.getByRole("button", { name: "Play trailer" })).toBeTruthy();
  });

  test("does not render empty metadata", () => {
    renderHero({ movie: { primaryTitle: "Unknown title" } });

    expect(screen.queryByText(/^Original title:/)).toBeNull();
    expect(screen.getAllByText("—")).toHaveLength(2);
  });

  test("toggles the watchlist and reflects selected state", async () => {
    const user = userEvent.setup();
    const { onToggleBookmark } = renderHero({ isBookmarked: true });

    await user.click(screen.getByRole("button", { name: "In watchlist" }));

    expect(onToggleBookmark).toHaveBeenCalledTimes(1);
  });

  test("opens rating and invokes share actions", async () => {
    const user = userEvent.setup();
    const { onOpenRating, onShare } = renderHero({ userRating: 7 });

    await user.click(screen.getByRole("button", { name: "Your rating: 7" }));
    await user.click(screen.getByRole("button", { name: "Share movie" }));

    expect(onOpenRating).toHaveBeenCalledTimes(1);
    expect(onShare).toHaveBeenCalledTimes(1);
  });

  test("disables pending actions", () => {
    renderHero({
      isBookmarkLoading: true,
      isRatingLoading: true,
      isShareDisabled: true,
    });

    expect(
      screen.getByRole("button", { name: "Add to watchlist" }),
    ).toHaveProperty("disabled", true);
    expect(screen.getByRole("button", { name: "Rate movie" })).toHaveProperty(
      "disabled",
      true,
    );
    expect(screen.getByRole("button", { name: "Share movie" })).toHaveProperty(
      "disabled",
      true,
    );
  });
});
