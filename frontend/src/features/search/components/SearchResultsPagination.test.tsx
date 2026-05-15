import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import SearchResultsPagination from "./SearchResultsPagination";

describe("SearchResultsPagination", () => {
  test("renders one-based pages and reports selected page", () => {
    const onPageChange = vi.fn();

    render(
      <SearchResultsPagination
        onPageChange={onPageChange}
        page={1}
        pageCount={4}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Go to page 3" }));

    expect(onPageChange).toHaveBeenCalledWith(3);
  });

  test("does not render when only one page exists", () => {
    const { container } = render(
      <SearchResultsPagination
        onPageChange={() => undefined}
        page={0}
        pageCount={1}
      />,
    );

    expect(container.textContent).toBe("");
  });
});
