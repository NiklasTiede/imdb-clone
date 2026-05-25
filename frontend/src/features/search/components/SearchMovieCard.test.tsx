import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import {
  MovieRecordMovieGenreEnum,
  MovieRecordMovieTypeEnum,
} from "../../../client/movies/generator-output";
import SearchMovieCard from "./SearchMovieCard";

describe("SearchMovieCard", () => {
  test("renders a poster-first search result card", () => {
    render(
      <MemoryRouter>
        <SearchMovieCard
          movie={{
            id: 2872718,
            primaryTitle: "Nightcrawler",
            startYear: 2014,
            runtimeMinutes: 117,
            movieType: MovieRecordMovieTypeEnum.Movie,
            movieGenre: new Set([MovieRecordMovieGenreEnum.Crime]),
            imdbRating: 7.8,
            posterImageToken: "nightcrawlerToken",
          }}
        />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: /nightcrawler/i }).getAttribute("href"),
    ).toBe("/movie?id=2872718");
    expect(screen.getByAltText("movie poster").getAttribute("src")).toMatch(
      /nightcrawlerToken_size_300x450\.webp/,
    );
    expect(screen.getByText("7.8")).toBeTruthy();
    expect(screen.getByText("2014 · 117 min")).toBeTruthy();
  });
});
