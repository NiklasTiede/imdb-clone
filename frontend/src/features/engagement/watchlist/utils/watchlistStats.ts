import { WatchedMovieRecord } from "../../../../client/movies/generator-output";
import { formatGenre } from "./watchlistFormat";

export type WatchlistStats = {
  averageRating: string;
  movieCount: number;
  topGenre: string;
  totalRuntime: string;
};

export const formatRuntime = (minutes: number): string => {
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  if (hours === 0) {
    return `${remainingMinutes}m`;
  }
  if (remainingMinutes === 0) {
    return `${hours}h`;
  }
  return `${hours}h ${remainingMinutes}m`;
};

export const buildWatchlistStats = (
  items: WatchedMovieRecord[],
): WatchlistStats => {
  const runtime = items.reduce(
    (sum, item) => sum + (item.movie?.runtimeMinutes ?? 0),
    0,
  );
  const ratings = items
    .map((item) => item.movie?.imdbRating)
    .filter((rating): rating is number => rating !== undefined);
  const genres = new Map<string, number>();

  items.forEach((item) => {
    Array.from(item.movie?.movieGenre ?? []).forEach((genre) => {
      genres.set(genre, (genres.get(genre) ?? 0) + 1);
    });
  });

  const topGenre =
    Array.from(genres.entries()).sort((a, b) => b[1] - a[1])[0]?.[0] ?? "—";

  return {
    averageRating:
      ratings.length === 0
        ? "—"
        : (
            ratings.reduce((sum, rating) => sum + rating, 0) / ratings.length
          ).toFixed(1),
    movieCount: items.length,
    topGenre: topGenre === "—" ? topGenre : formatGenre(topGenre),
    totalRuntime: formatRuntime(runtime),
  };
};
