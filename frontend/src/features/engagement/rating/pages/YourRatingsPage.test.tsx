import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { MovieRecordMovieGenreEnum } from "../../../../client/movies/generator-output";
import YourRatingsPage from "./YourRatingsPage";

vi.mock("../../../../shared/auth", () => ({
  getUsername: () => "ada",
}));

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

describe("YourRatingsPage", () => {
  let localStorageData: Record<string, string>;

  beforeEach(() => {
    localStorageData = {};
    Object.defineProperty(window, "localStorage", {
      configurable: true,
      value: {
        getItem: vi.fn((key: string) => localStorageData[key] ?? null),
        setItem: vi.fn((key: string, value: string) => {
          localStorageData[key] = value;
        }),
      },
    });
  });

  test("renders rated movies as a poster grid with stats", () => {
    const queryClient = makeQueryClient();
    queryClient.setQueryData(
      ["rating", "current-user", "ada", "movies", 0, 30],
      {
        content: [
          {
            movie: {
              id: 7,
              primaryTitle: "First Movie",
              startYear: 2014,
              movieGenre: new Set([MovieRecordMovieGenreEnum.Thriller]),
              movieType: "MOVIE",
              runtimeMinutes: 117,
              imdbRating: 7.8,
            },
            rating: 9,
          },
          {
            movie: {
              id: 9,
              primaryTitle: "Second Movie",
              startYear: 2008,
              movieGenre: new Set([MovieRecordMovieGenreEnum.Drama]),
              movieType: "MOVIE",
              runtimeMinutes: 102,
              imdbRating: 8.2,
            },
            rating: 7,
          },
        ],
      },
    );

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <YourRatingsPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("heading", { name: "Your Ratings" })).toBeTruthy();
    expect(screen.getByRole("grid", { name: "Rated movies" })).toBeTruthy();
    expect(screen.getByText("Your average")).toBeTruthy();
    expect(screen.getByText("8.0")).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "First Movie, 2014 · 117 min" }),
    ).toBeTruthy();
    expect(screen.getByLabelText("Your rating 9 out of 10")).toBeTruthy();
  });

  test("filters high scores and toggles list view", async () => {
    const user = userEvent.setup();
    const queryClient = makeQueryClient();
    queryClient.setQueryData(
      ["rating", "current-user", "ada", "movies", 0, 30],
      {
        content: [
          {
            movie: { id: 7, primaryTitle: "Loved Movie", startYear: 2014 },
            rating: 9,
          },
          {
            movie: { id: 9, primaryTitle: "Okay Movie", startYear: 2012 },
            rating: 6,
          },
        ],
      },
    );

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <YourRatingsPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await user.click(screen.getByRole("button", { name: "8+ only" }));
    await user.click(screen.getByRole("button", { name: "List view" }));

    expect(screen.getByText("Loved Movie")).toBeTruthy();
    expect(screen.queryByText("Okay Movie")).toBeNull();
    expect(screen.getByRole("list", { name: "Rated movies" })).toBeTruthy();
  });
});
