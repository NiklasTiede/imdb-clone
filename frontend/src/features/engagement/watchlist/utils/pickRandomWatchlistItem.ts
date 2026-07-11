import type { WatchlistItem } from "../model/watchlist";

export const pickRandomWatchlistItem = (
  items: WatchlistItem[],
  previousMovieId?: number,
): WatchlistItem | null => {
  const [firstItem] = items;
  if (!firstItem) {
    return null;
  }
  if (items.length === 1) {
    return firstItem;
  }

  const candidates = items.filter((item) => item.movieId !== previousMovieId);
  return candidates[Math.floor(Math.random() * candidates.length)] ?? firstItem;
};
