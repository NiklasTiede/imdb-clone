import { describe, expect, test, vi } from "vitest";
import { accountApi, moviesApi } from "../../../shared/api/moviesApi";
import { ratingQueries } from "./ratingQueries";

describe("ratingQueries", () => {
  test("builds a stable query key for the current user's rating of a movie", () => {
    expect(
      ratingQueries.userRatingForMovie({ movieId: 7, username: "ada" })
        .queryKey,
    ).toEqual(["rating", "current-user", "ada", "movie", 7]);
  });

  test("disables the query without a username or movie id", () => {
    expect(
      ratingQueries.userRatingForMovie({ movieId: 7, username: null }).enabled,
    ).toBe(false);
    expect(
      ratingQueries.userRatingForMovie({ movieId: null, username: "ada" })
        .enabled,
    ).toBe(false);
  });

  test("builds a stable query key for the current user's rated movies", () => {
    expect(
      ratingQueries.currentUserMovies({
        page: 0,
        size: 20,
        username: "ada",
      }).queryKey,
    ).toEqual(["rating", "current-user", "ada", "movies", 0, 20]);
  });

  test("loads rated movies and keeps the user's rating next to each movie", async () => {
    const ratingsSpy = vi
      .spyOn(accountApi, "getRatingsByAccount")
      .mockResolvedValue({
        data: {
          content: [
            { movieId: 7, rating: 8 },
            { movieId: 9, rating: 6 },
          ],
          last: true,
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1,
        },
      } as never);
    const moviesSpy = vi.spyOn(moviesApi, "getMoviesByIds").mockResolvedValue({
      data: {
        content: [
          { id: 9, primaryTitle: "Second Movie" },
          { id: 7, primaryTitle: "First Movie" },
        ],
      },
    } as never);

    const result = await ratingQueries
      .currentUserMovies({
        page: 0,
        size: 20,
        username: "ada",
      })
      .queryFn();

    expect(ratingsSpy).toHaveBeenCalledWith("ada", 0, 20);
    expect(moviesSpy).toHaveBeenCalledWith({ movieIds: [7, 9] }, 0, 2);
    expect(result.content).toEqual([
      {
        movie: { id: 7, primaryTitle: "First Movie" },
        rating: 8,
      },
      {
        movie: { id: 9, primaryTitle: "Second Movie" },
        rating: 6,
      },
    ]);
    expect(result.totalElements).toBe(2);

    ratingsSpy.mockRestore();
    moviesSpy.mockRestore();
  });
});
