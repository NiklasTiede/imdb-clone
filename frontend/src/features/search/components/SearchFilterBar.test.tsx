import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import { MovieSearchRequestMovieGenreEnum } from "../../../client/movies/generator-output";
import SearchFilterBar from "./SearchFilterBar";

describe("SearchFilterBar", () => {
  test("replaces the full year range when choosing an era", () => {
    const onChange = vi.fn();

    render(
      <SearchFilterBar
        filters={{ minStartYear: 1990 }}
        onChange={onChange}
        onClear={vi.fn()}
      />,
    );

    fireEvent.click(
      screen.getAllByRole("button", { name: "From 1990" })[0]!,
    );
    fireEvent.click(screen.getByRole("menuitem", { name: "2000s" }));

    expect(onChange).toHaveBeenCalledWith({ maxYear: 2009, minYear: 2000 });
  });

  test("replaces runtime bounds instead of accumulating them", () => {
    const onChange = vi.fn();

    render(
      <SearchFilterBar
        filters={{ maxRuntimeMinutes: 90 }}
        onChange={onChange}
        onClear={vi.fn()}
      />,
    );

    fireEvent.click(
      screen.getAllByRole("button", { name: "Until 90 min" })[0]!,
    );
    fireEvent.click(screen.getByRole("menuitem", { name: "2½+ hours" }));

    expect(onChange).toHaveBeenCalledWith({ maxRuntime: null, minRuntime: 150 });
  });

  test("shows only searchable catalog genres and clears all filters", () => {
    const onChange = vi.fn();
    const onClear = vi.fn();

    render(
      <SearchFilterBar
        filters={{
          minRuntimeMinutes: 150,
          minStartYear: 2020,
          movieGenre: new Set([MovieSearchRequestMovieGenreEnum.Drama]),
        }}
        onChange={onChange}
        onClear={onClear}
      />,
    );

    expect(screen.getAllByRole("button", { name: "Drama" })).toHaveLength(2);
    expect(screen.getAllByText("2020s")).toHaveLength(2);
    expect(screen.getAllByText("2½+ hours")).toHaveLength(2);
    expect(screen.queryByText("Movie & series")).toBeNull();

    fireEvent.click(screen.getAllByRole("button", { name: "Drama" })[0]!);

    expect(screen.getByRole("menuitem", { name: "Western" })).toBeTruthy();
    expect(screen.queryByRole("menuitem", { name: "Reality Tv" })).toBeNull();

    fireEvent.click(screen.getByRole("menuitem", { name: "All genres" }));
    fireEvent.click(screen.getByRole("button", { name: "Clear all" }));

    expect(onClear).toHaveBeenCalledTimes(1);
  });
});
