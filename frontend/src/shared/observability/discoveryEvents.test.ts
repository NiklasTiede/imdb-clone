import { beforeEach, describe, expect, test, vi } from "vitest";
import { apiHttpClient } from "../api/httpClient";
import {
  recordDiscoveryEvent,
  resetDiscoveryEventSessionForTests,
} from "./discoveryEvents";

describe("recordDiscoveryEvent", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    window.sessionStorage.clear();
    resetDiscoveryEventSessionForTests();
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
    expect(window.sessionStorage.getItem("imdb-clone.discovery-session-id")).toBe(
      firstPayload.sessionId,
    );
  });
});
