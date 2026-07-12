import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import SearchHeader from "./SearchHeader";

describe("SearchHeader", () => {
  test("renders query and total result count", () => {
    render(
      <SearchHeader
        onViewChange={() => undefined}
        query="Nightcrawler"
        totalCount={12}
        view="grid"
      />,
    );

    expect(
      screen.getByRole("heading", { name: 'Results for "Nightcrawler"' }),
    ).toBeTruthy();
    expect(screen.getByText("12 movies")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Grid view" })).toBeTruthy();
  });
});
