import type { RatedMovie } from "../api/ratingQueries";

export type RatingSort =
  | "score_desc"
  | "score_asc"
  | "imdb_desc"
  | "imdb_asc"
  | "title_asc";

export type ScoreRange = {
  label: string;
  max: number;
  min: number;
};

export const allScoresRange: ScoreRange = {
  label: "All scores",
  min: 1,
  max: 10,
};

export const scoreRanges: ScoreRange[] = [
  allScoresRange,
  { label: "8+ only", min: 8, max: 10 },
  { label: "5-7", min: 5, max: 7 },
];

export const ratingSortLabels: Record<RatingSort, string> = {
  score_desc: "Highest rated",
  score_asc: "Lowest rated",
  imdb_desc: "Highest IMDb rating",
  imdb_asc: "Lowest IMDb rating",
  title_asc: "Title A-Z",
};

export const filterRatedMovies = (
  items: RatedMovie[],
  range: ScoreRange,
): RatedMovie[] =>
  items.filter((item) => item.rating >= range.min && item.rating <= range.max);

export const sortRatedMovies = (
  items: RatedMovie[],
  sortBy: RatingSort,
): RatedMovie[] =>
  [...items].sort((left, right) => {
    switch (sortBy) {
      case "score_asc":
        return left.rating - right.rating;
      case "imdb_desc":
        return compareNumbers(right.movie.imdbRating, left.movie.imdbRating);
      case "imdb_asc":
        return compareNumbers(left.movie.imdbRating, right.movie.imdbRating);
      case "title_asc":
        return (left.movie.primaryTitle ?? "").localeCompare(
          right.movie.primaryTitle ?? "",
        );
      case "score_desc":
      default:
        return right.rating - left.rating;
    }
  });

const compareNumbers = (left?: number, right?: number): number =>
  (left ?? Number.NEGATIVE_INFINITY) - (right ?? Number.NEGATIVE_INFINITY);
