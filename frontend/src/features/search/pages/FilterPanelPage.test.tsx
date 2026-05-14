import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test } from "vitest";
import FilterPanelPage from "./FilterPanelPage";

describe("FilterPanelPage", () => {
  test("renders movie filter controls without placeholder copy", () => {
    render(
      <MemoryRouter>
        <FilterPanelPage />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { name: "Find your next movie" }),
    ).toBeTruthy();
    expect(screen.getByLabelText("Minimum start year")).toBeTruthy();
    expect(screen.getByLabelText("Movie type")).toBeTruthy();
    expect(screen.queryByText("Gilad Gray")).toBeNull();
    expect(screen.queryByText("FilterPanel")).toBeNull();
  });
});
