import type { QueryClient } from "@tanstack/react-query";
import { watchlistApi } from "../../../shared/api/moviesApi";

export type ToggleWatchlistVariables = {
  movieId: number;
  isBookmarked: boolean;
};

export const toggleWatchlistMutationOptions = (queryClient?: QueryClient) => ({
  mutationFn: async ({ movieId, isBookmarked }: ToggleWatchlistVariables) => {
    if (isBookmarked) {
      await watchlistApi.deleteWatchedMovie(movieId);
    } else {
      await watchlistApi.watchMovie(movieId);
    }
  },
  onSuccess: () => {
    queryClient?.invalidateQueries({ queryKey: ["watchlist"] });
  },
});

export const addToWatchlist = async (movieId: number) => {
  await watchlistApi.watchMovie(movieId);
};

export const removeFromWatchlist = async (movieId: number) => {
  await watchlistApi.deleteWatchedMovie(movieId);
};
