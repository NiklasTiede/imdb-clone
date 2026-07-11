import { describe, expect, test } from "vitest";
import { homeFeedQueryKey } from "../api/homeFeedQueries";

describe("home feed query key", () => {
  test("keeps one cache entry for a browser-document discovery session", () => {
    expect(homeFeedQueryKey("session-a")).toEqual(["home", "feed", "session-a"]);
  });
});
