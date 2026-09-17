import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import { defaultThemeId, getAppTheme } from "../../../theme";
import PosterMovieCard from "./PosterMovieCard";

const movie = {
  id: 42,
  primaryTitle: "A Beautiful Film",
  startYear: 2024,
  runtimeMinutes: 112,
  imdbRating: 8.4,
};

describe("PosterMovieCard", () => {
  test("keeps the title accessible without rendering it below the poster", () => {
    render(
      <MemoryRouter>
        <PosterMovieCard movie={movie} />
      </MemoryRouter>,
    );

    expect(screen.queryByText("A Beautiful Film")).toBeNull();
    expect(
      screen.getByRole("link", {
        name: "A Beautiful Film, 2024 · 112 min",
      }),
    ).toBeTruthy();
    expect(screen.getByText("2024 · 112 min")).toBeTruthy();
  });

  test("applies the theme's poster hover effect", () => {
    render(
      <MemoryRouter>
        <PosterMovieCard movie={movie} />
      </MemoryRouter>,
    );

    const css = Array.from(document.styleSheets)
      .flatMap((sheet) => Array.from(sheet.cssRules))
      .map((rule) => rule.cssText)
      .join(" ");
    expect(css).toContain(
      String(getAppTheme(defaultThemeId).effects.posterHover.transform),
    );
  });
});
