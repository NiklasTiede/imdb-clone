import { describe, expect, test } from "vitest";
import { MovieRecordMovieGenreEnum } from "../../../client/movies/generator-output";
import { buildWatchlistStats, formatRuntime } from "./watchlistStats";

describe("watchlistStats", () => {
  test("computes movie count, runtime, average rating, and top genre", () => {
    const stats = buildWatchlistStats([
      {
        movie: {
          runtimeMinutes: 100,
          imdbRating: 7,
          movieGenre: new Set([
            MovieRecordMovieGenreEnum.Horror,
            MovieRecordMovieGenreEnum.Thriller,
          ]),
        },
      },
      {
        movie: {
          runtimeMinutes: 95,
          imdbRating: 8,
          movieGenre: new Set([MovieRecordMovieGenreEnum.Thriller]),
        },
      },
      {
        movie: {
          runtimeMinutes: undefined,
          imdbRating: undefined,
          movieGenre: new Set([MovieRecordMovieGenreEnum.Action]),
        },
      },
    ]);

    expect(stats).toEqual({
      averageRating: "7.5",
      movieCount: 3,
      topGenre: "Thriller",
      totalRuntime: "3h 15m",
    });
  });

  test("formats runtimes without empty hours or minutes", () => {
    expect(formatRuntime(50)).toBe("50m");
    expect(formatRuntime(120)).toBe("2h");
    expect(formatRuntime(125)).toBe("2h 5m");
    expect(formatRuntime(0)).toBe("0m");
  });
});
