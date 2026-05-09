import { fireEvent, render, screen } from "@testing-library/react";
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

    fireEvent.keyDown(screen.getByRole("textbox", { name: "search" }), {
      key: "Enter",
      target: { value: "it follows" },
    });
    fireEvent.click(screen.getByRole("button", { name: "clear" }));

    expect(onSearch).toHaveBeenCalledWith("it follows");
    expect(onClear).toHaveBeenCalledOnce();
  });
});
