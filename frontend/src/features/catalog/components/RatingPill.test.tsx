import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { RatingPill, IMDB_GOLD, COMMUNITY_BLUE } from "./RatingPill";

describe("RatingPill", () => {
  test("renders the label, score, scale and formatted count", () => {
    render(
      <RatingPill
        label="IMDb rating"
        score={6.8}
        count={245000}
        starColor={IMDB_GOLD}
      />,
    );

    expect(screen.getByText("IMDb rating")).toBeTruthy();
    expect(screen.getByText("6.8")).toBeTruthy();
    expect(screen.getByText("/ 10 · 245k")).toBeTruthy();
  });

  test("renders a community pill with blue star and dash for missing values", () => {
    render(
      <RatingPill
        label="Community"
        score={undefined}
        count={undefined}
        starColor={COMMUNITY_BLUE}
      />,
    );

    expect(screen.getByText("Community")).toBeTruthy();
    expect(screen.getByText("—")).toBeTruthy();
    const star = screen.getByTestId("rating-pill-star") as HTMLElement;
    expect(star.style.color).toBe("rgb(122, 184, 255)");
    expect(COMMUNITY_BLUE).toBe("#7ab8ff");
  });
});
