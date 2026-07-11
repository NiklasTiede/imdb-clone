import { describe, expect, test } from "vitest";
import {
  MovieRecordMovieGenreEnum,
  MovieRecordMovieTypeEnum,
} from "../../../client/movies/generator-output";
import type { Movie } from "./movie";
import {
  formatMovieRuntime,
  formatMovieYear,
  getMovieGenreLabels,
  getMovieMetaItems,
  getOriginalTitle,
  humanizeMovieValue,
} from "./moviePresentation";

describe("movie presentation", () => {
  test("humanizes enum values and formats runtime", () => {
    expect(humanizeMovieValue("TV_MINI_SERIES")).toBe("Tv Mini Series");
    expect(formatMovieRuntime(142)).toBe("2h 22m");
    expect(formatMovieRuntime(120)).toBe("2h");
    expect(formatMovieRuntime(49)).toBe("49m");
    expect(formatMovieRuntime(0)).toBeNull();
  });

  test("formats year ranges only for series", () => {
    expect(
      formatMovieYear({
        movieType: MovieRecordMovieTypeEnum.TvSeries,
        startYear: 2008,
        endYear: 2013,
      }),
    ).toBe("2008–2013");
    expect(
      formatMovieYear({
        movieType: MovieRecordMovieTypeEnum.Movie,
        startYear: 2008,
        endYear: 2013,
      }),
    ).toBe("2008");
    expect(formatMovieYear({})).toBeNull();
  });

  test("builds metadata and genre labels without empty values", () => {
    const movie: Movie = {
      adult: true,
      movieGenre: new Set([
        MovieRecordMovieGenreEnum.SciFi,
        MovieRecordMovieGenreEnum.Drama,
      ]),
      movieType: MovieRecordMovieTypeEnum.Movie,
      runtimeMinutes: 142,
      startYear: 1994,
    };

    expect(getMovieMetaItems(movie)).toEqual([
      "1994",
      "Movie",
      "2h 22m",
      "18+",
    ]);
    expect(getMovieGenreLabels(movie)).toEqual(["Sci Fi", "Drama"]);
    expect(getMovieMetaItems({})).toEqual([]);
  });

  test("shows an original title only when it differs", () => {
    expect(
      getOriginalTitle({
        primaryTitle: "Spirited Away",
        originalTitle: "Sen to Chihiro no kamikakushi",
      }),
    ).toBe("Sen to Chihiro no kamikakushi");
    expect(
      getOriginalTitle({
        primaryTitle: "Arrival",
        originalTitle: "Arrival",
      }),
    ).toBeNull();
  });
});
