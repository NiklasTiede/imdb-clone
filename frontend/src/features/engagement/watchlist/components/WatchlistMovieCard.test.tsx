import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test, vi } from "vitest";
import { MovieRecordMovieGenreEnum } from "../../../../client/movies/generator-output";
import WatchlistMovieCard from "./WatchlistMovieCard";

describe("WatchlistMovieCard", () => {
  test("renders the watchlist item poster, metadata, and added date", () => {
    render(
      <MemoryRouter>
        <WatchlistMovieCard
          item={{
            addedAt: "2026-05-08T12:00:00Z",
            movieId: 2872718,
            movie: {
              id: 2872718,
              primaryTitle: "Nightcrawler",
              startYear: 2014,
              runtimeMinutes: 117,
              imdbRating: 7.8,
              posterImageToken: "nightcrawlerToken",
              movieGenre: new Set([MovieRecordMovieGenreEnum.Crime]),
            },
          }}
          onRemove={vi.fn()}
        />
      </MemoryRouter>,
    );

    expect(screen.getByRole("link", { name: /nightcrawler/i })).toBeTruthy();
    expect(screen.queryByText("Nightcrawler")).toBeNull();
    expect(screen.getByText("2014 · 117 min")).toBeTruthy();
    expect(screen.getByText(/^Added /)).toBeTruthy();
    expect(screen.getByAltText("movie poster").getAttribute("src")).toMatch(
      /nightcrawlerToken_size_300x450\.webp/,
    );
  });

  test("calls remove without navigating from the card", () => {
    const onRemove = vi.fn();
    render(
      <MemoryRouter>
        <WatchlistMovieCard
          item={{
            movieId: 2872718,
            movie: {
              id: 2872718,
              primaryTitle: "Nightcrawler",
            },
          }}
          onRemove={onRemove}
        />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: /remove from watchlist/i }));

    expect(onRemove).toHaveBeenCalledWith(2872718);
  });
});
