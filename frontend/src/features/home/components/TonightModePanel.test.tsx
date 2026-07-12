import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, test, vi } from "vitest";
import TonightModePanel from "./TonightModePanel";

const mocks = vi.hoisted(() => ({ getTonightPicks: vi.fn() }));

const tonightPicks = [
  {
    explanation: "Fits your time tonight.",
    movie: {
      id: 11,
      imdbRating: 8.1,
      primaryTitle: "First choice",
      runtimeMinutes: 98,
    },
  },
  {
    explanation: "A different mood.",
    movie: {
      id: 12,
      imdbRating: 7.9,
      primaryTitle: "Second choice",
      runtimeMinutes: 104,
    },
  },
  {
    explanation: "A great closer.",
    movie: {
      id: 13,
      imdbRating: 7.8,
      primaryTitle: "Third choice",
      runtimeMinutes: 112,
    },
  },
];

vi.mock("../api/tonightMode", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../api/tonightMode")>()),
  getTonightPicks: mocks.getTonightPicks,
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
  render(<TonightModePanel watchedMovieIds={new Set([4])} />, { wrapper });
};

describe("TonightModePanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getTonightPicks.mockResolvedValue({
      picks: tonightPicks,
      seed: "tonight-seed",
    });
  });

  test("sends constraints and renders exactly three explained choices", async () => {
    const user = userEvent.setup();
    renderPanel();

    await user.click(screen.getByText("Edge of seat"));
    await user.click(screen.getByText("Under 2 hours"));
    await user.click(screen.getByText("Thriller"));
    await user.click(
      screen.getByRole("button", { name: "Pick tonight's movies" }),
    );

    expect(
      await screen.findByRole("link", { name: /first choice/i }),
    ).toBeTruthy();
    expect(screen.getByRole("link", { name: /second choice/i })).toBeTruthy();
    expect(screen.getByRole("link", { name: /third choice/i })).toBeTruthy();
    expect(screen.queryByText("First choice")).toBeNull();
    expect(screen.getByText("Fits your time tonight.")).toBeTruthy();
    expect(mocks.getTonightPicks.mock.calls[0]?.[0]).toEqual(
      expect.objectContaining({
        excludedMovieIds: [4],
        maxRuntimeMinutes: 120,
        mood: "TENSE",
        movieGenres: new Set(["THRILLER"]),
      }),
    );
  });

  test("renders at most three choices in the swipeable results list", async () => {
    const user = userEvent.setup();
    mocks.getTonightPicks.mockResolvedValueOnce({
      picks: [
        ...tonightPicks,
        {
          explanation: "An extra result that should stay hidden.",
          movie: { id: 14, primaryTitle: "Fourth choice", runtimeMinutes: 90 },
        },
      ],
      seed: "tonight-seed",
    });
    renderPanel();

    await user.click(
      screen.getByRole("button", { name: "Pick tonight's movies" }),
    );

    const choices = await screen.findByRole("list", {
      name: "Tonight's movie choices",
    });
    expect(within(choices).getAllByRole("listitem")).toHaveLength(3);
    expect(screen.queryByRole("link", { name: /fourth choice/i })).toBeNull();
  });

  test("excludes prior choices when asking for another three", async () => {
    const user = userEvent.setup();
    renderPanel();

    await user.click(
      screen.getByRole("button", { name: "Pick tonight's movies" }),
    );
    await screen.findByRole("link", { name: /first choice/i });
    await user.click(screen.getByRole("button", { name: "Different three" }));

    const nextRequest = mocks.getTonightPicks.mock.calls.at(
      -1,
    )?.[0] as unknown as {
      excludedMovieIds: number[];
      seed?: string;
    };
    expect(nextRequest).toEqual(
      expect.objectContaining({ seed: "tonight-seed" }),
    );
    expect(new Set(nextRequest?.excludedMovieIds)).toEqual(
      new Set([4, 11, 12, 13]),
    );
  });

  test("keeps the current choices mounted while a replacement set is loading", async () => {
    const user = userEvent.setup();
    let resolveRefresh: (value: unknown) => void = () => undefined;
    mocks.getTonightPicks.mockReset();
    mocks.getTonightPicks
      .mockResolvedValueOnce({
        picks: [
          {
            explanation: "First round.",
            movie: { id: 11, primaryTitle: "First choice", runtimeMinutes: 98 },
          },
        ],
        seed: "tonight-seed",
      })
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveRefresh = resolve;
          }),
      );
    renderPanel();

    await user.click(
      screen.getByRole("button", { name: "Pick tonight's movies" }),
    );
    await screen.findByRole("link", { name: /first choice/i });
    await user.click(screen.getByRole("button", { name: "Different three" }));

    expect(screen.getByRole("link", { name: /first choice/i })).toBeTruthy();
    expect(screen.getByText("Finding three more…")).toBeTruthy();

    resolveRefresh({
      picks: [
        {
          explanation: "Second round.",
          movie: { id: 14, primaryTitle: "Fresh choice", runtimeMinutes: 104 },
        },
      ],
      seed: "tonight-seed",
    });

    expect(
      await screen.findByRole("link", { name: /fresh choice/i }),
    ).toBeTruthy();
  });

  test("clears a selected single-choice filter when it is clicked again", async () => {
    const user = userEvent.setup();
    renderPanel();

    await user.click(screen.getByText("Edge of seat"));
    await user.click(screen.getByText("Edge of seat"));
    await user.click(
      screen.getByRole("button", { name: "Pick tonight's movies" }),
    );

    expect(mocks.getTonightPicks.mock.calls[0]?.[0]).not.toHaveProperty("mood");
  });
});
