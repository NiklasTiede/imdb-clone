import type { RatedMovie } from "../api/ratingQueries";
import { formatGenre } from "../../watchlist";

export type RatingsStats = {
  averageRating: string;
  movieCount: number;
  topDecade: string;
  topGenre: string;
};

export const buildRatingsStats = (items: RatedMovie[]): RatingsStats => {
  const ratings = items.map((item) => item.rating);
  const average =
    ratings.length > 0
      ? ratings.reduce((sum, rating) => sum + rating, 0) / ratings.length
      : 0;

  return {
    averageRating: ratings.length > 0 ? average.toFixed(1) : "—",
    movieCount: items.length,
    topDecade: getTopDecade(items),
    topGenre: getTopGenre(items),
  };
};

const getTopGenre = (items: RatedMovie[]): string => {
  const counts = new Map<string, number>();

  items.forEach((item) => {
    Array.from(item.movie.movieGenre ?? []).forEach((genre) => {
      const value = String(genre);
      counts.set(value, (counts.get(value) ?? 0) + 1);
    });
  });

  const top = [...counts.entries()].sort(
    (left, right) => right[1] - left[1],
  )[0];
  return top ? formatGenre(top[0]) : "—";
};

const getTopDecade = (items: RatedMovie[]): string => {
  const counts = new Map<number, number>();

  items.forEach((item) => {
    if (item.movie.startYear !== undefined) {
      const decade = Math.floor(item.movie.startYear / 10) * 10;
      counts.set(decade, (counts.get(decade) ?? 0) + 1);
    }
  });

  const top = [...counts.entries()].sort(
    (left, right) => right[1] - left[1],
  )[0];
  return top ? `${top[0]}s` : "—";
};
