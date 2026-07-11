import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import BackdropImage from "./BackdropImage";

describe("BackdropImage", () => {
  test("renders a decorative large backdrop image", () => {
    render(<BackdropImage backdropImageToken="backdrop-token" />);

    const image = screen.getByRole("presentation", { hidden: true });
    expect(image.getAttribute("alt")).toBe("");
    expect(image.getAttribute("aria-hidden")).toBe("true");
    expect(image.getAttribute("src")).toContain(
      "movies/backdrops/backdrop-token_size_1280x720.webp",
    );
    expect(screen.getByTestId("movie-backdrop").dataset.hasImage).toBe(
      "true",
    );
  });

  test("keeps the fallback and removes a failed image", () => {
    render(<BackdropImage backdropImageToken="missing-token" />);

    fireEvent.error(screen.getByRole("presentation", { hidden: true }));

    expect(screen.queryByRole("presentation", { hidden: true })).toBeNull();
    expect(screen.getByTestId("movie-backdrop").dataset.hasImage).toBe(
      "false",
    );
  });

  test("renders only the fallback without a token", () => {
    render(<BackdropImage />);

    expect(screen.queryByRole("presentation", { hidden: true })).toBeNull();
    expect(screen.getByTestId("movie-backdrop").dataset.hasImage).toBe(
      "false",
    );
  });
});
