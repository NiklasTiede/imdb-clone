import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, test, vi } from "vitest";
import WatchlistDecisionPanel from "./WatchlistDecisionPanel";

const mocks = vi.hoisted(() => ({ watchlistTonight: vi.fn() }));

vi.mock("../../../../shared/api/moviesApi", () => ({
  recommendationApi: { watchlistTonight: mocks.watchlistTonight },
}));

const renderPanel = () => {
  const queryClient = new QueryClient({
    defaultOptions: { mutations: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
  render(
    <WatchlistDecisionPanel
      insights={{
        quickWatchCount: 3,
        totalMovies: 12,
        totalRuntimeMinutes: 720,
      }}
    />,
    { wrapper },
  );
};

describe("WatchlistDecisionPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.watchlistTonight.mockResolvedValue({
      data: {
        picks: [
          {
            explanation: "A strong match.",
            movie: { id: 12, primaryTitle: "Arrival", runtimeMinutes: 116 },
            role: "SAFE_BET",
          },
        ],
        seed: "watchlist-seed",
      },
    });
  });

  test("shows explained choices and reuses the seed with shown movies excluded", async () => {
    const user = userEvent.setup();
    renderPanel();

    await user.click(
      screen.getByRole("button", { name: "Recommend something new" }),
    );

    expect(await screen.findByText("Safe bet")).toBeTruthy();
    expect(screen.getByText("A strong match.")).toBeTruthy();
    expect(mocks.watchlistTonight).toHaveBeenCalledWith({
      excludedMovieIds: [],
    });

    await user.click(screen.getByText("Edge of seat"));
    await user.click(
      screen.getByRole("button", { name: "Refresh with these preferences" }),
    );

    expect(mocks.watchlistTonight).toHaveBeenLastCalledWith({
      excludedMovieIds: [12],
      mood: "TENSE",
      seed: "watchlist-seed",
    });
  });

  test("keeps the current choices visible while a replacement set is loading", async () => {
    const user = userEvent.setup();
    let resolveRefresh: (value: unknown) => void = () => undefined;
    mocks.watchlistTonight.mockReset();
    mocks.watchlistTonight
      .mockResolvedValueOnce({
        data: {
          picks: [
            {
              explanation: "First round.",
              movie: { id: 12, primaryTitle: "Arrival", runtimeMinutes: 116 },
              role: "SAFE_BET",
            },
          ],
          seed: "watchlist-seed",
        },
      })
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveRefresh = resolve;
          }),
      );
    renderPanel();

    await user.click(
      screen.getByRole("button", { name: "Recommend something new" }),
    );
    await screen.findByText("First round.");
    await user.click(screen.getByRole("button", { name: "Show three others" }));

    expect(screen.getByText("First round.")).toBeTruthy();
    resolveRefresh({
      data: {
        picks: [
          {
            explanation: "Second round.",
            movie: {
              id: 13,
              primaryTitle: "Fresh Arrival",
              runtimeMinutes: 101,
            },
            role: "WILD_CARD",
          },
        ],
        seed: "watchlist-seed",
      },
    });

    expect(await screen.findByText("Second round.")).toBeTruthy();
  });
});
