import { describe, expect, test } from "vitest";
import {
  MovieSearchRequestMovieGenreEnum,
  MovieSearchRequestMovieTypeEnum,
} from "../../../client/movies/generator-output";
import { parseSearchUrlState } from "./searchUrlState";

describe("parseSearchUrlState", () => {
  test("reads the current query parameter used by the app", () => {
    expect(parseSearchUrlState("?query=Nightcrawler")).toEqual({
      filters: {},
      page: 0,
      query: "Nightcrawler",
      sort: null,
    });
  });

  test("supports the planned q parameter and converts page to zero-based API pagination", () => {
    expect(parseSearchUrlState("?q=it%20follows&page=3")).toEqual({
      filters: {},
      page: 2,
      query: "it follows",
      sort: null,
    });
  });

  test("normalizes empty query and invalid page values", () => {
    expect(parseSearchUrlState("?query=%20%20&page=abc")).toEqual({
      filters: {},
      page: 0,
      query: null,
      sort: null,
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
      sort: "rating_desc",
    });
  });

  test("parses movie type filters from the filter page", () => {
    expect(parseSearchUrlState("?type=TV_SERIES&minYear=1995")).toEqual({
      filters: {
        minStartYear: 1995,
        movieType: MovieSearchRequestMovieTypeEnum.TvSeries,
      },
      page: 0,
      query: null,
      sort: null,
    });
  });
});
