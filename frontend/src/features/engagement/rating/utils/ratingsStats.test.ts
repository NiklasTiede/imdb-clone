import { describe, expect, test } from "vitest";
import { MovieRecordMovieGenreEnum } from "../../../../client/movies/generator-output";
import type { RatedMovie } from "../api/ratingQueries";
import { buildRatingsStats } from "./ratingsStats";

const ratedMovies: RatedMovie[] = [
  {
    rating: 9,
    movie: {
      id: 1,
      primaryTitle: "Nightcrawler",
      startYear: 2014,
      movieGenre: new Set([
        MovieRecordMovieGenreEnum.Thriller,
        MovieRecordMovieGenreEnum.Crime,
      ]),
    },
  },
  {
    rating: 7,
    movie: {
      id: 2,
      primaryTitle: "Arrival",
      startYear: 2016,
      movieGenre: new Set([
        MovieRecordMovieGenreEnum.SciFi,
        MovieRecordMovieGenreEnum.Drama,
      ]),
    },
  },
  {
    rating: 8,
    movie: {
      id: 3,
      primaryTitle: "Prisoners",
      startYear: 2013,
      movieGenre: new Set([
        MovieRecordMovieGenreEnum.Thriller,
        MovieRecordMovieGenreEnum.Drama,
      ]),
    },
  },
];

describe("ratingsStats", () => {
  test("computes count, average, top genre, and top decade", () => {
    expect(buildRatingsStats(ratedMovies)).toEqual({
      averageRating: "8.0",
      movieCount: 3,
      topDecade: "2010s",
      topGenre: "Thriller",
    });
  });
});
