import { describe, expect, it } from "vitest";
import { applicationDestination } from "./applicationNavigation";
import { applicationActionSchema } from "./concierge";

describe("application navigation", () => {
  it.each([
    ["home", "/"],
    ["settings", "/account-settings"],
    ["watchlist", "/your-watchlist"],
    ["ratings", "/your-ratings"],
  ] as const)(
    "opens %s without fabricating mutation receipts",
    (destination, route) => {
      const action = applicationActionSchema.parse({
        type: "open_page",
        destination,
      });
      expect(applicationDestination(action, true)).toBe(route);
      expect(applicationDestination(action, false)).toBe(
        destination === "home" ? "/" : "/login",
      );
    },
  );

  it("encodes all search criteria and resets pagination", () => {
    const action = applicationActionSchema.parse({
      type: "show_search_results",
      query: "time travel & space",
      genres: ["DRAMA", "SCI_FI"],
      movieType: "MOVIE",
      minStartYear: 1990,
      maxStartYear: 1999,
      minRuntimeMinutes: 0,
      maxRuntimeMinutes: 120,
    });
    const url = new URL(
      applicationDestination(action, false),
      "https://example.invalid",
    );
    expect(url.pathname).toBe("/movie-search");
    expect(url.searchParams.get("query")).toBe("time travel & space");
    expect(url.searchParams.getAll("genre")).toEqual(["DRAMA", "SCI_FI"]);
    expect(url.searchParams.get("movieType")).toBe("MOVIE");
    expect(url.searchParams.get("minYear")).toBe("1990");
    expect(url.searchParams.get("maxYear")).toBe("1999");
    expect(url.searchParams.get("minRuntime")).toBe("0");
    expect(url.searchParams.get("maxRuntime")).toBe("120");
    expect(url.searchParams.has("page")).toBe(false);
  });

  it.each([
    { type: "open_page", destination: "https://example.invalid" },
    { type: "open_page", destination: "settings", url: "/admin" },
    { type: "show_search_results", query: "x", genres: ["UNKNOWN"] },
    { type: "show_search_results", query: "x", movieType: "UNKNOWN" },
    { type: "show_search_results", query: "x", minStartYear: -1 },
  ])("rejects unsupported destinations and filters: %j", (action) => {
    expect(applicationActionSchema.safeParse(action).success).toBe(false);
  });
});
