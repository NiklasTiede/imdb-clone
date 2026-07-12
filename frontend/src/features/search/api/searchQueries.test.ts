import {
  MovieSearchRequestMovieGenreEnum,
  type MovieSearchRequest,
} from "../../../client/movies/generator-output";
import { normalizeSearchFiltersForKey, searchQueries } from "./searchQueries";

describe("searchQueries", () => {
  it("builds a stable movie search query key", () => {
    expect(
      searchQueries.movies({
        filters: {},
        page: 0,
        query: "it follows",
        size: 20,
      }).queryKey,
    ).toEqual([
      "search",
      "movies",
      "it follows",
      {
        maxRuntimeMinutes: null,
        maxStartYear: null,
        minRuntimeMinutes: null,
        minStartYear: null,
        movieGenre: [],
        movieType: null,
      },
      0,
      20,
    ]);
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

  it("keeps each selected genre in a distinct cache key", () => {
    const horror = normalizeSearchFiltersForKey({
      movieGenre: new Set([MovieSearchRequestMovieGenreEnum.Horror]),
    });
    const drama = normalizeSearchFiltersForKey({
      movieGenre: new Set([MovieSearchRequestMovieGenreEnum.Drama]),
    });

    expect(horror).not.toEqual(drama);
    expect(horror.movieGenre).toEqual([MovieSearchRequestMovieGenreEnum.Horror]);
    expect(drama.movieGenre).toEqual([MovieSearchRequestMovieGenreEnum.Drama]);
  });
});
