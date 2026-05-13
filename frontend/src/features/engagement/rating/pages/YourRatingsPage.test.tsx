import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { describe, expect, test, vi } from "vitest";
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
  test("renders rated movies with the user's rating", () => {
    const queryClient = makeQueryClient();
    queryClient.setQueryData(["rating", "current-user", "ada", "movies", 0, 20], {
      content: [
        {
          movie: {
            id: 7,
            primaryTitle: "First Movie",
            startYear: 2014,
            movieType: "MOVIE",
            runtimeMinutes: 117,
            imdbRating: 7.8,
          },
          rating: 8,
        },
      ],
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <YourRatingsPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("heading", { name: "Your Ratings" })).toBeTruthy();
    expect(screen.getByText("First Movie")).toBeTruthy();
    expect(screen.getByText("Your rating: 8/10")).toBeTruthy();
  });
});
