import { describe, expect, test, vi } from "vitest";
import {
  MovieSearchRequestMovieGenreEnum,
  type MovieRecord,
} from "../client/movies/generator-output";
import { searchApi } from "../shared/api/moviesApi";
import { genreMoviesQuery } from "./useMoviesByGenre";

describe("genreMoviesQuery", () => {
  test("fetches recent movies for a genre and returns the highest-rated titles", async () => {
    const movies: MovieRecord[] = [
      { id: 1, primaryTitle: "Low", imdbRating: 5.5 },
      { id: 2, primaryTitle: "High", imdbRating: 8.4 },
      { id: 3, primaryTitle: "Missing rating" },
    ];
    const searchSpy = vi.spyOn(searchApi, "search").mockResolvedValue({
      data: { content: movies },
    } as Awaited<ReturnType<typeof searchApi.search>>);

    const query = genreMoviesQuery({
      genre: MovieSearchRequestMovieGenreEnum.Horror,
      minStartYear: 2016,
    });
    const result = await query.queryFn();

    expect(searchSpy).toHaveBeenCalledWith(
      "",
      {
        minStartYear: 2016,
        movieGenre: new Set([MovieSearchRequestMovieGenreEnum.Horror]),
      },
      0,
      20,
    );
    expect(result.map((movie) => movie.primaryTitle)).toEqual([
      "High",
      "Low",
      "Missing rating",
    ]);
    expect(result).toHaveLength(3);

    searchSpy.mockRestore();
  });
});
