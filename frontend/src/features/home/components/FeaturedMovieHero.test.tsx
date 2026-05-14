import { render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import FeaturedMovieHero from "./FeaturedMovieHero";

describe("FeaturedMovieHero", () => {
  test("shows an explicit fallback when the featured movie cannot load", () => {
    render(
      <FeaturedMovieHero
        error
        movie={null}
        onToggleBookmark={vi.fn()}
        onViewMovie={vi.fn()}
      />,
    );

    expect(screen.getByText("Featured movie unavailable")).toBeTruthy();
    expect(
      screen.getByText("The homepage is still ready to explore below."),
    ).toBeTruthy();
  });
});
