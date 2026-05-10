import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import SearchEmptyState from "./SearchEmptyState";

describe("SearchEmptyState", () => {
  test("renders a no-results message", () => {
    render(<SearchEmptyState />);

    expect(screen.getByRole("heading", { name: "No movies found" })).toBeTruthy();
    expect(
      screen.getByText("Try adjusting your search term or filters."),
    ).toBeTruthy();
  });
});
