import { describe, expect, test } from "vitest";
import { MovieSearchRequestMovieGenreEnum } from "../../../client/movies/generator-output";
import { createSearchUrl, parseSearchUrlState } from "./searchUrlState";

describe("parseSearchUrlState", () => {
  test("reads the current query parameter used by the app", () => {
    expect(parseSearchUrlState("?query=Nightcrawler")).toEqual({
      filters: {},
      page: 0,
      query: "Nightcrawler",
    });
  });

  test("supports q and converts page to zero-based API pagination", () => {
    expect(parseSearchUrlState("?q=it%20follows&page=3")).toEqual({
      filters: {},
      page: 2,
      query: "it follows",
    });
  });

  test("normalizes empty query and invalid page values", () => {
    expect(parseSearchUrlState("?query=%20%20&page=abc")).toEqual({
      filters: {},
      page: 0,
      query: null,
    });
  });

  test("parses homepage view-all genre filters", () => {
    expect(
      parseSearchUrlState("?genre=HORROR&minYear=2016&sort=rating_desc&page=2"),
    ).toEqual({
      filters: {
        minStartYear: 2016,
        movieGenre: new Set([MovieSearchRequestMovieGenreEnum.Horror]),
      },
      page: 1,
      query: null,
    });
  });

  test("ignores retired movie-type and sort parameters", () => {
    expect(
      parseSearchUrlState(
        "?type=TV_SERIES&minYear=1995&maxYear=2018&minRuntime=80&maxRuntime=140",
      ),
    ).toEqual({
      filters: {
        maxRuntimeMinutes: 140,
        maxStartYear: 2018,
        minRuntimeMinutes: 80,
        minStartYear: 1995,
      },
      page: 0,
      query: null,
    });
  });

  test("updates filter params, retires legacy params, and resets pagination", () => {
    expect(
      createSearchUrl("?q=the&page=4&type=TV_SERIES&sort=rating_desc", {
        genre: MovieSearchRequestMovieGenreEnum.Horror,
      }),
    ).toBe("?q=the&genre=HORROR");
  });

  test("removes empty filter params and keeps explicit page changes", () => {
    expect(
      createSearchUrl("?q=the&genre=HORROR&sort=rating_desc", {
        genre: null,
        page: 3,
      }),
    ).toBe("?q=the&page=3");
  });
});
