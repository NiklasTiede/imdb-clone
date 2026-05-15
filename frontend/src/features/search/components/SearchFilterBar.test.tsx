import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import {
  MovieSearchRequestMovieGenreEnum,
  MovieSearchRequestMovieTypeEnum,
} from "../../../client/movies/generator-output";
import SearchFilterBar from "./SearchFilterBar";

describe("SearchFilterBar", () => {
  test("renders active filter pills and clears all filters", () => {
    const onChange = vi.fn();
    const onClear = vi.fn();

    render(
      <SearchFilterBar
        filters={{
          maxStartYear: 2010,
          minRuntimeMinutes: 90,
          minStartYear: 1990,
          movieGenre: new Set([MovieSearchRequestMovieGenreEnum.Drama]),
          movieType: MovieSearchRequestMovieTypeEnum.Movie,
        }}
        onChange={onChange}
        onClear={onClear}
      />,
    );

    expect(screen.getByRole("button", { name: /drama/i })).toBeTruthy();
    expect(screen.getByRole("button", { name: /1990-2010/i })).toBeTruthy();
    expect(screen.getByRole("button", { name: /movie/i })).toBeTruthy();
    expect(screen.getByRole("button", { name: /90\+ min/i })).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: /clear filters/i }));

    expect(onClear).toHaveBeenCalledTimes(1);
  });
});
