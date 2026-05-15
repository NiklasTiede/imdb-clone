import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import SearchLoadingSlot, { searchLoadingSlotSx } from "./SearchLoadingSlot";

describe("SearchLoadingSlot", () => {
  test("reserves progress height even when search is idle", () => {
    render(<SearchLoadingSlot loading={false} />);

    expect(screen.queryByRole("progressbar")).toBeNull();
    expect(searchLoadingSlotSx.minHeight).toBe(4);
  });

  test("renders the progress bar inside the reserved slot while loading", () => {
    render(<SearchLoadingSlot loading />);

    expect(
      screen.getByRole("progressbar", { name: "Loading search results" }),
    ).toBeTruthy();
    expect(searchLoadingSlotSx.minHeight).toBe(4);
  });
});
