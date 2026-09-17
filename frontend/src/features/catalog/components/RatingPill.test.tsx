import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { defaultThemeId, getAppTheme } from "../../../theme";
import { RatingPill } from "./RatingPill";

const toRgb = (hex: string) =>
  `rgb(${[1, 3, 5].map((index) => parseInt(hex.slice(index, index + 2), 16)).join(", ")})`;

describe("RatingPill", () => {
  test("renders the label, score, scale and formatted count", () => {
    render(<RatingPill label="IMDb rating" score={6.8} count={245000} />);

    expect(screen.getByText("IMDb rating")).toBeTruthy();
    expect(screen.getByText("6.8")).toBeTruthy();
    expect(screen.getByText("/ 10 · 245k")).toBeTruthy();
  });

  test("renders a dash for missing values with the theme star colour", () => {
    render(
      <RatingPill label="Community" score={undefined} count={undefined} />,
    );

    expect(screen.getByText("Community")).toBeTruthy();
    expect(screen.getByText("—")).toBeTruthy();
    const star = screen.getByTestId("rating-pill-star");
    expect(getComputedStyle(star).color).toBe(
      toRgb(getAppTheme(defaultThemeId).tokens.star),
    );
  });
});
