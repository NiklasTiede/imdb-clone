import { describe, expect, test } from "vitest";
import { MovieSearchRequestMovieGenreEnum } from "../../../client/movies/generator-output";
import { getHomeMinStartYear, homeGenreRows } from "./HomePage";

describe("homeGenreRows", () => {
  test("uses drama as the first homepage carousel", () => {
    expect(homeGenreRows[0]).toMatchObject({
      genre: MovieSearchRequestMovieGenreEnum.Drama,
      title: "Top drama",
      viewAllGenre: "DRAMA",
    });
  });

  test("uses a 30-year lookback for homepage carousel searches", () => {
    expect(getHomeMinStartYear(new Date("2026-05-10T00:00:00Z"))).toBe(1996);
  });
});
