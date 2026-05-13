import { describe, expect, test, vi } from "vitest";
import type { MovieRecord } from "../../../client/movies/generator-output";
import { moviesApi } from "../../../shared/api/moviesApi";
import { featuredMovieQuery } from "./useFeaturedMovie";

describe("featuredMovieQuery", () => {
  test("filters to poster-backed highly rated movies and picks the daily movie", async () => {
    const movies: MovieRecord[] = [
      { id: 1, primaryTitle: "No poster", imdbRating: 9 },
      {
        id: 2,
        primaryTitle: "Featured",
        imdbRating: 8.1,
        imageUrlToken: "featured-poster",
      },
      {
        id: 3,
        primaryTitle: "Too low",
        imdbRating: 7.4,
        imageUrlToken: "low-poster",
      },
    ];
    const moviesSpy = vi.spyOn(moviesApi, "getMoviesByIds").mockResolvedValue({
      data: { content: movies },
    } as Awaited<ReturnType<typeof moviesApi.getMoviesByIds>>);

    const query = featuredMovieQuery(new Date("2026-05-10T00:00:00Z"));
    const result = await query.queryFn();

    expect(moviesSpy).toHaveBeenCalledWith({}, 0, 30);
    expect(result?.primaryTitle).toBe("Featured");

    moviesSpy.mockRestore();
  });

  test("returns null when no eligible featured movie exists", async () => {
    const moviesSpy = vi.spyOn(moviesApi, "getMoviesByIds").mockResolvedValue({
      data: { content: [{ id: 1, primaryTitle: "No poster", imdbRating: 9 }] },
    } as Awaited<ReturnType<typeof moviesApi.getMoviesByIds>>);

    const query = featuredMovieQuery(new Date("2026-05-10T00:00:00Z"));

    await expect(query.queryFn()).resolves.toBeNull();
    moviesSpy.mockRestore();
  });
});
