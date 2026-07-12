import CloseIcon from "@mui/icons-material/CloseSharp";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test, vi } from "vitest";
import { MovieRecordMovieGenreEnum } from "../../../client/movies/generator-output";
import MovieListRow from "./MovieListRow";
import MovieListView from "./MovieListView";

describe("MovieListView", () => {
  test("renders a compact linked movie row", () => {
    render(
      <MemoryRouter>
        <MovieListView
          ariaLabel="Rated movies"
        >
          <MovieListRow
            movie={{
              id: 2872718,
              primaryTitle: "Nightcrawler",
              startYear: 2014,
              runtimeMinutes: 117,
              imdbRating: 7.8,
              movieGenre: new Set([MovieRecordMovieGenreEnum.Crime]),
              posterImageToken: "nightcrawlerToken",
            }}
            primaryRating={{ value: 8, variant: "user" }}
            secondaryRating={{ value: 7.8, variant: "imdb" }}
            timestamp="Added today"
          />
        </MovieListView>
      </MemoryRouter>,
    );

    expect(screen.getByRole("list", { name: "Rated movies" })).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /nightcrawler/i }).getAttribute("href"),
    ).toBe("/movie?id=2872718");
    expect(screen.getByText("Crime")).toBeTruthy();
    expect(screen.getByText("2014 · 117 min")).toBeTruthy();
    expect(screen.getByText("Added today")).toBeTruthy();
    expect(screen.getByLabelText("You rating 8")).toBeTruthy();
    expect(screen.getByLabelText("IMDb rating 7.8")).toBeTruthy();
  });

  test("keeps row actions separate from navigation", () => {
    const onDelete = vi.fn();

    render(
      <MemoryRouter>
        <MovieListView
          ariaLabel="Watchlist movies"
        >
          <MovieListRow
            action={{
              ariaLabel: "Remove from watchlist",
              color: "danger",
              icon: <CloseIcon fontSize="small" />,
              onClick: onDelete,
            }}
            movie={{
              id: 1,
              primaryTitle: "Tropic Thunder",
            }}
          />
        </MovieListView>
      </MemoryRouter>,
    );

    fireEvent.click(
      screen.getByRole("button", { name: "Remove from watchlist" }),
    );

    expect(onDelete).toHaveBeenCalledTimes(1);
  });
});
