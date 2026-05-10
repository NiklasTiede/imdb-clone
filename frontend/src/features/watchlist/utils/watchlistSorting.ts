import { WatchedMovieRecord } from "../../../client/movies/generator-output";

export type WatchlistSort =
  | "addedAt_desc"
  | "addedAt_asc"
  | "rating_desc"
  | "rating_asc"
  | "runtime_desc"
  | "runtime_asc"
  | "title_asc";

export const sortLabels: Record<WatchlistSort, string> = {
  addedAt_desc: "Recently added",
  addedAt_asc: "Oldest added",
  rating_desc: "Highest IMDb rating",
  rating_asc: "Lowest IMDb rating",
  runtime_desc: "Longest runtime",
  runtime_asc: "Shortest runtime",
  title_asc: "Title A-Z",
};

export const sortWatchlistItems = (
  items: WatchedMovieRecord[],
  sortBy: WatchlistSort,
): WatchedMovieRecord[] =>
  [...items].sort((left, right) => {
    switch (sortBy) {
      case "addedAt_asc":
        return compareDates(left.addedAt, right.addedAt);
      case "rating_desc":
        return compareNumbers(right.movie?.imdbRating, left.movie?.imdbRating);
      case "rating_asc":
        return compareNumbers(left.movie?.imdbRating, right.movie?.imdbRating);
      case "runtime_desc":
        return compareNumbers(
          right.movie?.runtimeMinutes,
          left.movie?.runtimeMinutes,
        );
      case "runtime_asc":
        return compareNumbers(
          left.movie?.runtimeMinutes,
          right.movie?.runtimeMinutes,
        );
      case "title_asc":
        return (left.movie?.primaryTitle ?? "").localeCompare(
          right.movie?.primaryTitle ?? "",
        );
      case "addedAt_desc":
      default:
        return compareDates(right.addedAt, left.addedAt);
    }
  });

const compareDates = (left?: string, right?: string): number =>
  new Date(left ?? 0).getTime() - new Date(right ?? 0).getTime();

const compareNumbers = (left?: number, right?: number): number =>
  (left ?? Number.NEGATIVE_INFINITY) - (right ?? Number.NEGATIVE_INFINITY);
