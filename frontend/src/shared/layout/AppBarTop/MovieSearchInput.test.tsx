import "@testing-library/jest-dom/vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import MovieSearchInput from "./MovieSearchInput";

describe("MovieSearchInput", () => {
  it("submits and clears the current search query", () => {
    const onQueryChange = vi.fn();
    const onSearch = vi.fn();
    const onClear = vi.fn();

    render(
      <MovieSearchInput
        query="it follows"
        onQueryChange={onQueryChange}
        onSearch={onSearch}
        onClear={onClear}
      />,
    );

    const searchInput = screen.getByRole("textbox", { name: "search movies" });
    expect(searchInput).toHaveAttribute(
      "placeholder",
      "Search a title or describe a movie",
    );

    fireEvent.keyDown(searchInput, {
      key: "Enter",
      target: { value: "it follows" },
    });
    fireEvent.click(screen.getByRole("button", { name: "clear" }));

    expect(onSearch).toHaveBeenCalledWith("it follows");
    expect(onClear).toHaveBeenCalledOnce();
  });

  it("focuses the search input with the keyboard shortcut", async () => {
    const user = userEvent.setup();

    render(
      <MovieSearchInput
        query=""
        onQueryChange={vi.fn()}
        onSearch={vi.fn()}
        onClear={vi.fn()}
      />,
    );

    await user.keyboard("{Control>}k{/Control}");

    expect(
      screen.getByRole("textbox", { name: "search movies" }),
    ).toHaveFocus();
  });

  it("blurs the search input when submitting with Enter", () => {
    render(
      <MovieSearchInput
        query="alien"
        onQueryChange={vi.fn()}
        onSearch={vi.fn()}
        onClear={vi.fn()}
      />,
    );
    const searchInput = screen.getByRole("textbox", { name: "search movies" });
    searchInput.focus();

    fireEvent.keyDown(searchInput, {
      key: "Enter",
      target: { value: "alien" },
    });

    expect(searchInput).not.toHaveFocus();
  });
});
