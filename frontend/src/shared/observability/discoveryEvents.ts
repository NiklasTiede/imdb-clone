import { readRenamedStorage } from "../storage/readRenamedStorage";
import { apiHttpClient } from "../api/httpClient";

export type DiscoveryEventType =
  | "MOVIE_DISMISSED"
  | "MOVIE_OPEN"
  | "RATING_SUBMITTED"
  | "SECTION_IMPRESSION"
  | "WATCHLIST_ADDED";

type DiscoveryEvent = {
  eventType: DiscoveryEventType;
  feedInstanceId: string;
  movieId?: number;
  position?: number;
  sectionId: string;
  strategyVersion: string;
};

const discoverySessionStorageKey = "popcorn-society.discovery-session-id";
let inMemorySessionId: string | undefined;

const createOpaqueId = () => {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `discovery-${Date.now()}-${Math.random().toString(36).slice(2)}`;
};

const getDiscoverySessionId = () => {
  if (typeof window === "undefined") {
    return (inMemorySessionId ??= createOpaqueId());
  }

  try {
    const stored = readRenamedStorage(
      window.sessionStorage, discoverySessionStorageKey, "imdb-clone.discovery-session-id",
    );
    if (stored) {
      return stored;
    }
    const sessionId = createOpaqueId();
    window.sessionStorage.setItem(discoverySessionStorageKey, sessionId);
    return sessionId;
  } catch {
    return (inMemorySessionId ??= createOpaqueId());
  }
};

/** Sends only product interaction metadata; the backend hashes the opaque session and feed ids. */
export const recordDiscoveryEvent = (event: DiscoveryEvent): void => {
  void apiHttpClient
    .post("/api/v1/recommendations/discovery-events", {
      ...event,
      eventId: createOpaqueId(),
      sessionId: getDiscoverySessionId(),
    })
    .catch(() => undefined);
};

export const resetDiscoveryEventSessionForTests = () => {
  inMemorySessionId = undefined;
  window.sessionStorage.removeItem(discoverySessionStorageKey);
};
