import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import Synopsis from "./Synopsis";

describe("Synopsis", () => {
  test("renders the movie description", () => {
    render(<Synopsis text="A driven freelancer enters crime journalism." />);

    expect(screen.getByRole("heading", { name: "Synopsis" })).toBeTruthy();
    expect(
      screen.getByText("A driven freelancer enters crime journalism."),
    ).toBeTruthy();
  });

  test("renders a fallback when the description is missing", () => {
    render(<Synopsis text={undefined} />);

    expect(screen.getByText("No synopsis available.")).toBeTruthy();
  });
});
