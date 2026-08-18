import "@testing-library/jest-dom/vitest";
import { ThemeProvider } from "@mui/material";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, vi } from "vitest";
import { authSession } from "../../../shared/auth";
import { installLocalStorageMock } from "../../../test/installLocalStorageMock";
import { appTheme } from "../../../theme";
import ConciergeExperience from "./ConciergeExperience";

const conversationId = "1234567890abcdef1234567890abcdef";

const event = (type: string, sequence: number, payload: object): string =>
  `event: ${type}\nid: ${sequence}\ndata: ${JSON.stringify({ type, sequence, ...payload })}\n\n`;

const streamResponse = (): Response =>
  new Response(
    event("status", 1, { status: "searching_catalog" }) +
      event("movie-card", 2, {
        movie: {
          movieId: 42,
          primaryTitle: "Arrival",
          originalTitle: "Arrival",
          movieType: "MOVIE",
          startYear: 2016,
          runtimeMinutes: 116,
          genres: ["DRAMA", "SCI_FI"],
          imdbRating: 7.9,
          imdbRatingCount: 800000,
          description: "A linguist meets visitors from another world.",
          posterImageToken: null,
          explanation: "A thoughtful science-fiction drama.",
        },
      }) +
      event("text", 3, { delta: "Arrival is grounded in the catalog." }) +
      event("usage", 4, {
        usage: {
          model: "deterministic-test",
          requests: 2,
          toolCalls: 1,
          inputTokens: 100,
          outputTokens: 20,
          totalTokens: 120,
          estimatedCostUsd: "0.001",
          costAvailable: true,
        },
      }) +
      event("completion", 5, { conversationId, outcome: "success" }),
    { headers: { "Content-Type": "text/event-stream" } },
  );

let unmountExperience: (() => void) | undefined;

const renderExperience = () =>
  (unmountExperience = render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <ConciergeExperience />
      </MemoryRouter>
    </ThemeProvider>,
  ).unmount);

describe("ConciergeExperience", () => {
  beforeEach(() => {
    unmountExperience = undefined;
    installLocalStorageMock();
    authSession.completeBootstrap(null);
  });

  afterEach(() => {
    unmountExperience?.();
    authSession.resetForTests();
    vi.unstubAllGlobals();
  });

  it("is public and automatically renders grounded cards from the stream", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ conversationId }), {
          status: 201,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(streamResponse());
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderExperience();

    await user.click(
      screen.getByRole("button", { name: "Ask the Movie Concierge" }),
    );
    expect(
      screen.getByRole("heading", { name: "What fits tonight?" }),
    ).toBeVisible();

    await user.type(
      screen.getByRole("textbox", { name: "Ask the Movie Concierge" }),
      "Find Arrival",
    );
    await user.click(
      screen.getByRole("button", { name: "Send concierge message" }),
    );

    expect(
      await screen.findByText("Arrival is grounded in the catalog."),
    ).toBeVisible();
    expect(screen.getByTestId("concierge-movie-card")).toBeVisible();
    expect(screen.getByRole("link", { name: "Arrival" })).toHaveAttribute(
      "href",
      "/movie?id=42",
    );
    expect(screen.getByText("120 tokens")).toBeVisible();

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    const firstRequest = fetchMock.mock.calls[0];
    const secondRequest = fetchMock.mock.calls[1];
    expect(firstRequest?.[0]).toBe("/concierge-api/v1/conversations");
    expect(secondRequest?.[0]).toContain(
      `/conversations/${conversationId}/messages`,
    );
    const firstHeaders = new Headers(firstRequest?.[1]?.headers);
    const secondHeaders = new Headers(secondRequest?.[1]?.headers);
    expect(firstHeaders.get("X-Concierge-Client-ID")).toMatch(
      /^browser-[a-f0-9-]{36}:anonymous$/,
    );
    expect(secondHeaders.get("X-Concierge-Client-ID")).toBe(
      firstHeaders.get("X-Concierge-Client-ID"),
    );
  });
});
