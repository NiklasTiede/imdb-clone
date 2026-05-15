import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import { MovieRecordMovieTypeEnum } from "../../../client/movies/generator-output";
import SearchMovieList from "./SearchMovieList";

describe("SearchMovieList", () => {
  test("renders search results in the shared movie list view", () => {
    render(
      <MemoryRouter>
        <SearchMovieList
          movies={[
            {
              id: 1,
              primaryTitle: "First Movie",
              imdbRating: 8.1,
              movieType: MovieRecordMovieTypeEnum.Movie,
            },
          ]}
        />
      </MemoryRouter>,
    );

    expect(screen.getByRole("list", { name: "Search results" })).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /first movie/i }).getAttribute("href"),
    ).toBe("/movie?id=1");
    expect(screen.getByLabelText("IMDb rating 8.1")).toBeTruthy();
  });
});
