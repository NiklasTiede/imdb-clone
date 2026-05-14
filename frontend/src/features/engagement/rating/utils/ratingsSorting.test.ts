import { describe, expect, test } from "vitest";
import type { RatedMovie } from "../api/ratingQueries";
import { filterRatedMovies, sortRatedMovies } from "./ratingsSorting";

const items: RatedMovie[] = [
  {
    rating: 6,
    movie: { id: 1, primaryTitle: "Zodiac", imdbRating: 7.7 },
  },
  {
    rating: 9,
    movie: { id: 2, primaryTitle: "Arrival", imdbRating: 7.9 },
  },
  {
    rating: 8,
    movie: { id: 3, primaryTitle: "Prisoners", imdbRating: 8.1 },
  },
];

describe("ratingsSorting", () => {
  test("sorts rated movies by user score and title", () => {
    expect(
      sortRatedMovies(items, "score_desc").map((item) => item.movie.id),
    ).toEqual([2, 3, 1]);
    expect(
      sortRatedMovies(items, "title_asc").map((item) => item.movie.id),
    ).toEqual([2, 3, 1]);
  });

  test("filters rated movies by score range", () => {
    expect(
      filterRatedMovies(items, { label: "8+ only", max: 10, min: 8 }).map(
        (item) => item.movie.id,
      ),
    ).toEqual([2, 3]);
  });
});
