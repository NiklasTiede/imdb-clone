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
  if (typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return `discovery-${Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("")}`;
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
  try {
    void apiHttpClient
      .post("/api/v1/recommendations/discovery-events", {
        ...event,
        eventId: createOpaqueId(),
        sessionId: getDiscoverySessionId(),
      })
      .catch(() => undefined);
  } catch {
    // Telemetry must not interrupt user actions when secure randomness is unavailable.
  }
};

export const resetDiscoveryEventSessionForTests = () => {
  inMemorySessionId = undefined;
  window.sessionStorage.removeItem(discoverySessionStorageKey);
};
