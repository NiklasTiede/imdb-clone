import "@testing-library/jest-dom/vitest";
import { ThemeProvider } from "@mui/material";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useLocation } from "react-router";
import { afterEach, beforeEach, vi } from "vitest";
import { authSession } from "../../../shared/auth";
import { installLocalStorageMock } from "../../../test/installLocalStorageMock";
import { appTheme } from "../../../theme";
import ConciergeExperience from "./ConciergeExperience";

const conversationId = "1234567890abcdef1234567890abcdef";

const event = (type: string, sequence: number, payload: object): string =>
  `event: ${type}\nid: ${sequence}\ndata: ${JSON.stringify({ type, sequence, ...payload })}\n\n`;

const streamResponse = (extraEvents = "", completionSequence = 5): Response =>
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
      event("text", 3, {
        delta: "The movie is **_Arrival_ (2016)**.",
      }) +
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
      extraEvents +
      event("completion", completionSequence, {
        conversationId,
        outcome: "success",
      }),
    { headers: { "Content-Type": "text/event-stream" } },
  );

let unmountExperience: (() => void) | undefined;

const LocationProbe = () => {
  const location = useLocation();
  return (
    <output aria-label="Current route">{`${location.pathname}${location.search}`}</output>
  );
};

const renderExperience = () =>
  (unmountExperience = render(
    <ThemeProvider theme={appTheme}>
      <MemoryRouter>
        <ConciergeExperience />
        <LocationProbe />
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
      await screen.findByText("Arrival", { selector: "em" }),
    ).toBeVisible();
    expect(screen.queryByText(/\*\*_/)).not.toBeInTheDocument();
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

  it("offers capability discovery as the featured first action", async () => {
    const capabilityResponse = new Response(
      event("status", 1, { status: "thinking" }) +
        event("text", 2, {
          delta:
            "I can search the catalog, show grounded details, find similar movies, choose tonight, and open a movie page.",
        }) +
        event("completion", 3, { conversationId, outcome: "success" }),
      { headers: { "Content-Type": "text/event-stream" } },
    );
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ conversationId }), { status: 201 }),
      )
      .mockResolvedValueOnce(capabilityResponse);
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderExperience();

    await user.click(
      screen.getByRole("button", { name: "Ask the Movie Concierge" }),
    );
    await user.click(
      screen.getByRole("button", { name: "See what the Concierge can do" }),
    );

    expect(await screen.findByText(/I can search the catalog/)).toBeVisible();
    const messageRequest = fetchMock.mock.calls[1];
    const requestBody = messageRequest?.[1]?.body;
    expect(typeof requestBody).toBe("string");
    if (typeof requestBody !== "string") {
      throw new TypeError("Expected a serialized concierge request body");
    }
    expect(JSON.parse(requestBody)).toEqual({
      message: "What can you do for me?",
    });
  });

  it("closes the overlay and opens the app-owned route for a grounded action", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ conversationId }), { status: 201 }),
      )
      .mockResolvedValueOnce(
        streamResponse(
          event("ui-action", 5, {
            action: { type: "open_movie", movieId: 42 },
          }),
          6,
        ),
      );
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderExperience();

    await user.click(
      screen.getByRole("button", { name: "Ask the Movie Concierge" }),
    );
    await user.type(
      screen.getByRole("textbox", { name: "Ask the Movie Concierge" }),
      "Open Arrival",
    );
    await user.click(
      screen.getByRole("button", { name: "Send concierge message" }),
    );

    await waitFor(() =>
      expect(screen.getByLabelText("Current route")).toHaveTextContent(
        "/movie?id=42",
      ),
    );
    await waitFor(() =>
      expect(
        screen.queryByRole("dialog", { name: "Movie Concierge" }),
      ).not.toBeInTheDocument(),
    );
  });

  it("does not navigate when an action arrives before its grounded card", async () => {
    const unsafeStream = new Response(
      event("ui-action", 1, {
        action: { type: "open_movie", movieId: 42 },
      }) + event("completion", 2, { conversationId, outcome: "success" }),
      { headers: { "Content-Type": "text/event-stream" } },
    );
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ conversationId }), { status: 201 }),
      )
      .mockResolvedValueOnce(unsafeStream);
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderExperience();

    await user.click(
      screen.getByRole("button", { name: "Ask the Movie Concierge" }),
    );
    await user.type(
      screen.getByRole("textbox", { name: "Ask the Movie Concierge" }),
      "Open Arrival",
    );
    await user.click(
      screen.getByRole("button", { name: "Send concierge message" }),
    );

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    expect(screen.getByLabelText("Current route")).toHaveTextContent("/");
    expect(
      screen.getByRole("dialog", { name: "Movie Concierge" }),
    ).toBeVisible();
  });

  it("rejects an action that carries a model-supplied URL", async () => {
    const maliciousStream = streamResponse(
      event("ui-action", 5, {
        action: {
          type: "open_movie",
          movieId: 42,
          url: "https://attacker.example/movie/42",
        },
      }),
      6,
    );
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ conversationId }), { status: 201 }),
      )
      .mockResolvedValueOnce(maliciousStream);
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderExperience();

    await user.click(
      screen.getByRole("button", { name: "Ask the Movie Concierge" }),
    );
    await user.type(
      screen.getByRole("textbox", { name: "Ask the Movie Concierge" }),
      "Open Arrival at a supplied URL",
    );
    await user.click(
      screen.getByRole("button", { name: "Send concierge message" }),
    );

    expect(
      await screen.findByText(
        "The Movie Concierge returned an invalid response.",
      ),
    ).toBeVisible();
    expect(screen.getByLabelText("Current route")).toHaveTextContent("/");
    expect(
      screen.getByRole("dialog", { name: "Movie Concierge" }),
    ).toBeVisible();
  });
});
