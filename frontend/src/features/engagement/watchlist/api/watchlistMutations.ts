import type { QueryClient, QueryKey } from "@tanstack/react-query";
import type { PagedResponseWatchedMovieRecord } from "../../../../client/movies/generator-output";
import { watchlistApi } from "../../../../shared/api/moviesApi";
import { watchlistQueryKeys } from "./watchlistQueries";

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
    queryClient?.invalidateQueries({ queryKey: watchlistQueryKeys.all });
  },
});

export const addToWatchlist = async (movieId: number) => {
  await watchlistApi.watchMovie(movieId);
};

export const removeFromWatchlist = async (movieId: number) => {
  await watchlistApi.deleteWatchedMovie(movieId);
};

type RemoveFromWatchlistMutationOptionsParams = {
  onRemoveError: () => void;
  onRemoved: (movieId: number) => void;
  queryClient: QueryClient;
  watchlistQueryKey: QueryKey;
};

type RemoveFromWatchlistContext = {
  previous?: PagedResponseWatchedMovieRecord;
};

export const removeFromWatchlistMutationOptions = ({
  onRemoveError,
  onRemoved,
  queryClient,
  watchlistQueryKey,
}: RemoveFromWatchlistMutationOptionsParams) => ({
  mutationFn: removeFromWatchlist,
  onMutate: async (movieId: number): Promise<RemoveFromWatchlistContext> => {
    await queryClient.cancelQueries({ queryKey: watchlistQueryKey });
    const previous =
      queryClient.getQueryData<PagedResponseWatchedMovieRecord>(
        watchlistQueryKey,
      );

    queryClient.setQueryData<PagedResponseWatchedMovieRecord>(
      watchlistQueryKey,
      (current) =>
        current
          ? {
              ...current,
              content: current.content?.filter(
                (item) => (item.movieId ?? item.movie?.id) !== movieId,
              ),
              totalElements: Math.max((current.totalElements ?? 1) - 1, 0),
            }
          : current,
    );

    return { previous };
  },
  onError: (
    _error: unknown,
    _movieId: number,
    context: RemoveFromWatchlistContext | undefined,
  ) => {
    if (context?.previous) {
      queryClient.setQueryData(watchlistQueryKey, context.previous);
    }
    onRemoveError();
  },
  onSuccess: (_data: unknown, movieId: number) => {
    onRemoved(movieId);
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: watchlistQueryKeys.all });
  },
});
