import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import PosterMovieCard from "./PosterMovieCard";

const movie = {
  id: 42,
  primaryTitle: "A Beautiful Film",
  startYear: 2024,
  runtimeMinutes: 112,
  imdbRating: 8.4,
};

describe("PosterMovieCard", () => {
  test("keeps the title accessible when carousel captions hide it", () => {
    render(
      <MemoryRouter>
        <PosterMovieCard movie={movie} showTitle={false} />
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
});
