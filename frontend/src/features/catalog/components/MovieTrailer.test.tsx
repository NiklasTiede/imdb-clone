import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test } from "vitest";
import MovieTrailer from "./MovieTrailer";

describe("MovieTrailer", () => {
  test("does not render an empty player for a missing or invalid key", () => {
    const { rerender } = render(
      <MovieTrailer movieTitle="Arrival" youtubeVideoKey={undefined} />,
    );
    expect(screen.queryByTestId("movie-trailer")).toBeNull();

    rerender(
      <MovieTrailer movieTitle="Arrival" youtubeVideoKey="not a key" />,
    );
    expect(screen.queryByTestId("movie-trailer")).toBeNull();
  });

  test("uses local media until the user starts playback", async () => {
    const user = userEvent.setup();
    render(
      <MovieTrailer
        backdropImageToken="backdrop-token"
        movieTitle="Arrival"
        youtubeVideoKey="abcDEF123_-"
      />,
    );

    expect(screen.queryByTitle("Arrival trailer")).toBeNull();
    expect(screen.getByRole("button", { name: "Play trailer" })).toBeTruthy();

    await user.click(screen.getByRole("button", { name: "Play trailer" }));

    const iframe = screen.getByTitle("Arrival trailer");
    expect(iframe.getAttribute("src")).toBe(
      "https://www.youtube-nocookie.com/embed/abcDEF123_-?autoplay=1&playsinline=1&rel=0",
    );
    expect(iframe.hasAttribute("allowfullscreen")).toBe(true);
  });
});
