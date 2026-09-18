import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { apiHttpClient } from "../api/httpClient";
import {
  recordDiscoveryEvent,
  resetDiscoveryEventSessionForTests,
} from "./discoveryEvents";

describe("recordDiscoveryEvent", () => {
  const event = {
    eventType: "MOVIE_OPEN" as const,
    feedInstanceId: "feed-123",
    movieId: 7,
    sectionId: "new-and-noteworthy",
    strategyVersion: "home-structured-v1",
  };

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  beforeEach(() => {
    vi.restoreAllMocks();
    window.sessionStorage.clear();
    resetDiscoveryEventSessionForTests();
  });

  test("uses secure random bytes when randomUUID is unavailable", () => {
    const getRandomValues = crypto.getRandomValues.bind(crypto);
    vi.stubGlobal("crypto", { getRandomValues });
    vi.spyOn(Math, "random").mockImplementation(() => {
      throw new Error("Insecure randomness must not be used");
    });
    const post = vi.spyOn(apiHttpClient, "post").mockResolvedValue({} as never);

    expect(() => {
      recordDiscoveryEvent(event);
      recordDiscoveryEvent(event);
    }).not.toThrow();

    expect(post).toHaveBeenCalledTimes(2);
    const first = post.mock.calls[0]?.[1] as { eventId: string; sessionId: string };
    const second = post.mock.calls[1]?.[1] as { eventId: string; sessionId: string };
    expect(first.eventId).toMatch(/^discovery-[a-f0-9]{32}$/);
    expect(first.sessionId).toMatch(/^discovery-[a-f0-9]{32}$/);
    expect(first.eventId).not.toBe(second.eventId);
    expect(first.eventId).not.toBe(first.sessionId);
    expect(first.sessionId).toBe(second.sessionId);
  });

  test.each([undefined, {}])("skips telemetry without secure randomness: %s", (cryptoApi) => {
    vi.stubGlobal("crypto", cryptoApi);
    const post = vi.spyOn(apiHttpClient, "post").mockResolvedValue({} as never);

    expect(() => recordDiscoveryEvent(event)).not.toThrow();
    expect(post).not.toHaveBeenCalled();
    expect(window.sessionStorage.getItem("popcorn-society.discovery-session-id")).toBeNull();
  });

  test("does not interrupt user actions when secure randomness fails", () => {
    vi.stubGlobal("crypto", {
      getRandomValues: () => { throw new Error("Randomness unavailable"); },
    });
    const post = vi.spyOn(apiHttpClient, "post").mockResolvedValue({} as never);

    expect(() => recordDiscoveryEvent(event)).not.toThrow();
    expect(post).not.toHaveBeenCalled();
  });

  test("keeps an in-memory session when session storage is blocked", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("Storage blocked");
    });
    const post = vi.spyOn(apiHttpClient, "post").mockResolvedValue({} as never);

    recordDiscoveryEvent(event);
    recordDiscoveryEvent(event);

    expect(post).toHaveBeenCalledTimes(2);
    const first = post.mock.calls[0]?.[1] as { eventId: string; sessionId: string };
    const second = post.mock.calls[1]?.[1] as { eventId: string; sessionId: string };
    expect(first.sessionId).toBeTruthy();
    expect(first.sessionId).toBe(second.sessionId);
    expect(first.eventId).not.toBe(second.eventId);
  });

  test("uses one opaque session for multiple events and never waits for telemetry", () => {
    const post = vi.spyOn(apiHttpClient, "post").mockResolvedValue({} as never);

    recordDiscoveryEvent({
      eventType: "SECTION_IMPRESSION",
      feedInstanceId: "feed-123",
      position: 0,
      sectionId: "new-and-noteworthy",
      strategyVersion: "home-structured-v1",
    });
    recordDiscoveryEvent({
      eventType: "MOVIE_OPEN",
      feedInstanceId: "feed-123",
      movieId: 7,
      position: 2,
      sectionId: "new-and-noteworthy",
      strategyVersion: "home-structured-v1",
    });

    expect(post).toHaveBeenCalledTimes(2);
    const firstPayload = post.mock.calls[0]?.[1] as { eventId: string; sessionId: string };
    const secondPayload = post.mock.calls[1]?.[1] as { eventId: string; sessionId: string };
    expect(firstPayload.eventId).not.toBe(secondPayload.eventId);
    expect(firstPayload.sessionId).toBe(secondPayload.sessionId);
    expect(window.sessionStorage.getItem("popcorn-society.discovery-session-id")).toBe(
      firstPayload.sessionId,
    );
  });
});

test("preserves an existing discovery session during rebranding", () => {
  window.sessionStorage.clear();
  window.sessionStorage.setItem("imdb-clone.discovery-session-id", "existing-session");
  const post = vi.spyOn(apiHttpClient, "post").mockResolvedValue({} as never);
  recordDiscoveryEvent({
    eventType: "MOVIE_OPEN", feedInstanceId: "feed-123", movieId: 7,
    sectionId: "new-and-noteworthy", strategyVersion: "home-structured-v1",
  });
  expect(post).toHaveBeenLastCalledWith(expect.any(String), expect.objectContaining({ sessionId: "existing-session" }));
  expect(window.sessionStorage.getItem("popcorn-society.discovery-session-id")).toBe("existing-session");
  vi.restoreAllMocks();
});
