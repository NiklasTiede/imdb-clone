import { describe, expect, test, vi } from "vitest";
import {
  MovieSearchRequestMovieTypeEnum,
  type MovieRecord,
} from "../../../client/movies/generator-output";
import { moviesApi, searchApi } from "../../../shared/api/moviesApi";
import { featuredMovieQuery } from "./useFeaturedMovie";

describe("featuredMovieQuery", () => {
  test("filters to poster-backed highly rated movies and picks the daily movie", async () => {
    const movies: MovieRecord[] = [
      { id: 1, primaryTitle: "No poster", imdbRating: 9 },
      {
        id: 2,
        primaryTitle: "Featured",
        imdbRating: 8.1,
        posterImageToken: "featured-poster",
      },
      {
        id: 3,
        primaryTitle: "Too low",
        imdbRating: 7.4,
        imageUrlToken: "low-poster",
      },
    ];
    const searchSpy = vi.spyOn(searchApi, "search").mockResolvedValue({
      data: { content: movies },
    } as Awaited<ReturnType<typeof searchApi.search>>);
    const movieSpy = vi.spyOn(moviesApi, "getMovieById").mockResolvedValue({
      data: {
        id: 2,
        primaryTitle: "Featured",
        description: "Full synopsis from movie details.",
        imdbRating: 8.1,
        imageUrlToken: "featured-poster",
      },
    } as Awaited<ReturnType<typeof moviesApi.getMovieById>>);

    const query = featuredMovieQuery(new Date("2026-05-10T00:00:00Z"));
    const result = await query.queryFn();

    expect(searchSpy).toHaveBeenCalledWith(
      "",
      {
        minStartYear: 1996,
        movieType: MovieSearchRequestMovieTypeEnum.Movie,
      },
      0,
      30,
    );
    expect(movieSpy).toHaveBeenCalledWith(2);
    expect(result?.primaryTitle).toBe("Featured");
    expect(result?.description).toBe("Full synopsis from movie details.");

    searchSpy.mockRestore();
    movieSpy.mockRestore();
  });

  test("returns null when no eligible featured movie exists", async () => {
    const searchSpy = vi.spyOn(searchApi, "search").mockResolvedValue({
      data: { content: [{ id: 1, primaryTitle: "No poster", imdbRating: 9 }] },
    } as Awaited<ReturnType<typeof searchApi.search>>);

    const query = featuredMovieQuery(new Date("2026-05-10T00:00:00Z"));

    await expect(query.queryFn()).resolves.toBeNull();
    searchSpy.mockRestore();
  });
});
