import type { InfiniteData, QueryClient, QueryKey } from "@tanstack/react-query";
import type {
  PagedResponseWatchedMovieRecord,
  WatchlistLibraryResponse,
} from "../../../../client/movies/generator-output";
import { watchlistApi } from "../../../../shared/api/moviesApi";
import { watchlistQueryKeys } from "./watchlistQueries";

export type ToggleWatchlistVariables = {
  movieId: number;
  isBookmarked: boolean;
};

export const toggleWatchlistMutationOptions = (
  queryClient?: QueryClient,
  { onAdded }: { onAdded?: (movieId: number) => void } = {},
) => ({
  mutationFn: async ({ movieId, isBookmarked }: ToggleWatchlistVariables) => {
    if (isBookmarked) {
      await watchlistApi.deleteWatchedMovie(movieId);
    } else {
      await watchlistApi.watchMovie(movieId);
    }
  },
  onSuccess: async (_data: unknown, variables: ToggleWatchlistVariables) => {
    await queryClient?.invalidateQueries({ queryKey: watchlistQueryKeys.all });
    if (!variables.isBookmarked) {
      onAdded?.(variables.movieId);
    }
  },
});

export const removeFromWatchlist = async (movieId: number) => {
  await watchlistApi.deleteWatchedMovie(movieId);
};

type RemoveFromWatchlistMutationOptionsParams = {
  onRemoveError: () => void;
  queryClient: QueryClient;
  watchlistQueryKey: QueryKey;
};

type RemoveFromWatchlistContext = {
  previous?: WatchlistQueryData;
};

type WatchlistQueryData =
  | PagedResponseWatchedMovieRecord
  | InfiniteData<WatchlistLibraryResponse>;

const removeMovieFromPage = (
  page: PagedResponseWatchedMovieRecord,
  movieId: number,
): PagedResponseWatchedMovieRecord => ({
  ...page,
  content: (page.content ?? []).filter(
    (item) => (item.movieId ?? item.movie?.id) !== movieId,
  ),
  totalElements: Math.max((page.totalElements ?? 1) - 1, 0),
});

const isInfiniteLibraryData = (
  data: WatchlistQueryData,
): data is InfiniteData<WatchlistLibraryResponse> => "pages" in data;

export const removeFromWatchlistMutationOptions = ({
  onRemoveError,
  queryClient,
  watchlistQueryKey,
}: RemoveFromWatchlistMutationOptionsParams) => ({
  mutationFn: removeFromWatchlist,
  onMutate: async (movieId: number): Promise<RemoveFromWatchlistContext> => {
    await queryClient.cancelQueries({ queryKey: watchlistQueryKey });
    const previous = queryClient.getQueryData<WatchlistQueryData>(
      watchlistQueryKey,
    );

    queryClient.setQueryData<WatchlistQueryData>(
      watchlistQueryKey,
      (current) =>
        !current
          ? current
          : isInfiniteLibraryData(current)
            ? {
                ...current,
                pages: current.pages.map((page) =>
                  page.items
                    ? {
                        ...page,
                        items: removeMovieFromPage(page.items, movieId),
                      }
                    : page,
                ),
              }
            : removeMovieFromPage(current, movieId),
    );

    return previous === undefined ? {} : { previous };
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
  onSettled: async () => {
    await queryClient.invalidateQueries({ queryKey: watchlistQueryKeys.all });
  },
});
