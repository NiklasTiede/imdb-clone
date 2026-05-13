import { WatchedMovieRecord } from "../../../../client/movies/generator-output";

export const pickRandomWatchlistItem = (
  items: WatchedMovieRecord[],
  previousMovieId?: number,
): WatchedMovieRecord | null => {
  if (items.length === 0) {
    return null;
  }
  if (items.length === 1) {
    return items[0];
  }

  const candidates = items.filter((item) => item.movieId !== previousMovieId);
  return candidates[Math.floor(Math.random() * candidates.length)] ?? items[0];
};
