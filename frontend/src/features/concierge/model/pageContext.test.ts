import { describe, expect, it } from "vitest";
import { pageContextFromLocation } from "./pageContext";
describe("browser page context", () => {
  it("sends only allowlisted movie hints, never credentials or arbitrary URL fields", () => {
    expect(
      pageContextFromLocation({
        pathname: "/movie",
        search: "?id=6&token=private&redirect=https://example.invalid",
        hash: "#trailer",
      }),
    ).toEqual({ page: "movie", movieId: 6, section: "trailer" });
    expect(
      pageContextFromLocation({
        pathname: "/reset-password",
        search: "?token=private",
        hash: "#private",
      }),
    ).toEqual({ page: "unknown" });
  });
  it.each(["-1", "0", "NaN", "9007199254740992", "https://example.invalid"])(
    "drops invalid movie IDs (%s)",
    (id) => {
      expect(
        pageContextFromLocation({
          pathname: "/movie",
          search: `?id=${id}`,
          hash: "",
        }),
      ).toEqual({ page: "movie", section: "overview" });
    },
  );
  it("bounds search text and distinguishes personal pages without copying their contents", () => {
    expect(
      pageContextFromLocation({
        pathname: "/movie-search",
        search: `?query=${"x".repeat(300)}`,
        hash: "",
      }),
    ).toEqual({ page: "search", searchQuery: "x".repeat(200) });
    expect(
      pageContextFromLocation({
        pathname: "/your-ratings",
        search: "?accountId=999",
        hash: "",
      }),
    ).toEqual({ page: "ratings" });
  });
});
