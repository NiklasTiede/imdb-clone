import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import SearchHeader from "./SearchHeader";

describe("SearchHeader", () => {
  test("renders query and total result count", () => {
    render(<SearchHeader query="Nightcrawler" totalCount={12} />);

    expect(
      screen.getByRole("heading", { name: 'Results for "Nightcrawler"' }),
    ).toBeTruthy();
    expect(screen.getByText("12 movies")).toBeTruthy();
  });
});
