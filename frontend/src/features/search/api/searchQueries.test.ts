import { searchQueries } from "./searchQueries";
import {
  MovieSearchRequestMovieGenreEnum,
  type MovieSearchRequest,
} from "../../../client/movies/generator-output";

describe("searchQueries", () => {
  it("builds a stable movie search query key", () => {
    expect(
      searchQueries.movies({
        filters: {},
        page: 0,
        query: "it follows",
        size: 20,
      }).queryKey,
    ).toEqual(["search", "movies", "it follows", {}, 0, 20]);
  });

  it("disables movie search when no query exists", () => {
    expect(
      searchQueries.movies({
        filters: {},
        page: 0,
        query: null,
        size: 20,
      }).enabled,
    ).toBe(false);
  });

  it("normalizes blank movie search query strings", () => {
    expect(
      searchQueries.movies({
        filters: {},
        page: 0,
        query: "   ",
        size: 20,
      }).enabled,
    ).toBe(false);
  });

  it("enables movie search for filter-only homepage view-all links", () => {
    const filters: MovieSearchRequest = {
      minStartYear: 2016,
      movieGenre: new Set([MovieSearchRequestMovieGenreEnum.Horror]),
    };

    expect(
      searchQueries.movies({
        filters,
        page: 0,
        query: null,
        size: 20,
      }).enabled,
    ).toBe(true);
  });
});
