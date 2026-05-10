import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import { MovieRecordMovieTypeEnum } from "../../../client/movies/generator-output";
import SearchMovieGrid, { searchMovieGridSx } from "./SearchMovieGrid";

describe("SearchMovieGrid", () => {
  test("renders movies in a search results grid", () => {
    render(
      <MemoryRouter>
        <SearchMovieGrid
          movies={[
            {
              id: 1,
              primaryTitle: "First Movie",
              movieType: MovieRecordMovieTypeEnum.Movie,
            },
            {
              id: 2,
              primaryTitle: "Second Movie",
              movieType: MovieRecordMovieTypeEnum.Movie,
            },
          ]}
        />
      </MemoryRouter>,
    );

    expect(screen.getByRole("grid", { name: "Search results" })).toBeTruthy();
    expect(screen.getByRole("link", { name: /first movie/i })).toBeTruthy();
    expect(screen.getByRole("link", { name: /second movie/i })).toBeTruthy();
  });

  test("caps desktop columns so a single result does not stretch full width", () => {
    expect(searchMovieGridSx.gridTemplateColumns).toEqual({
      xs: "repeat(2, minmax(0, 1fr))",
      sm: "repeat(auto-fill, minmax(150px, 180px))",
      md: "repeat(auto-fill, minmax(160px, 190px))",
    });
    expect(searchMovieGridSx.justifyContent).toBe("start");
  });
});
