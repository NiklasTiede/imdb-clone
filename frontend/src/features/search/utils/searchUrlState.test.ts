import { describe, expect, test } from "vitest";
import { parseSearchUrlState } from "./searchUrlState";

describe("parseSearchUrlState", () => {
  test("reads the current query parameter used by the app", () => {
    expect(parseSearchUrlState("?query=Nightcrawler")).toEqual({
      page: 0,
      query: "Nightcrawler",
    });
  });

  test("supports the planned q parameter and converts page to zero-based API pagination", () => {
    expect(parseSearchUrlState("?q=it%20follows&page=3")).toEqual({
      page: 2,
      query: "it follows",
    });
  });

  test("normalizes empty query and invalid page values", () => {
    expect(parseSearchUrlState("?query=%20%20&page=abc")).toEqual({
      page: 0,
      query: null,
    });
  });
});
