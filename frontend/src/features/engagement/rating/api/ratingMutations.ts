import type {
  InfiniteData,
  QueryClient,
  QueryKey,
} from "@tanstack/react-query";
import type { RatingLibraryResponse } from "../../../../client/movies/generator-output";
import { ratingApi } from "../../../../shared/api/moviesApi";

const allRatingQueries = ["rating"] as const;

export type RateMovieVariables = {
  movieId: number;
  score: number | null;
};

export const rateMovieMutationOptions = (queryClient?: QueryClient) => ({
  mutationFn: async ({ movieId, score }: RateMovieVariables) => {
    if (score === null) {
      await ratingApi.deleteRating(movieId);
    } else {
      await ratingApi.rateMovie(movieId, score);
    }
  },
  onSuccess: async (_data: unknown, variables: RateMovieVariables) => {
    await Promise.all([
      queryClient?.invalidateQueries({
        queryKey: allRatingQueries,
      }),
      queryClient?.invalidateQueries({
        queryKey: ["catalog", "movie", variables.movieId],
      }),
    ]);
  },
});

type RemoveRatingMutationOptionsParams = {
  onRemoveError: () => void;
  queryClient: QueryClient;
  ratingQueryKey: QueryKey;
};

type RemoveRatingContext = {
  previous?: InfiniteData<RatingLibraryResponse>;
};

const removeMovieFromLibrary = (
  data: InfiniteData<RatingLibraryResponse>,
  movieId: number,
): InfiniteData<RatingLibraryResponse> => ({
  ...data,
  pages: data.pages.map((page) =>
    page.items
      ? {
          ...page,
          items: {
            ...page.items,
            content: (page.items.content ?? []).filter(
              (item) => (item.movieId ?? item.movie?.id) !== movieId,
            ),
            totalElements: Math.max((page.items.totalElements ?? 1) - 1, 0),
          },
        }
      : page,
  ),
});

export const removeRatingMutationOptions = ({
  onRemoveError,
  queryClient,
  ratingQueryKey,
}: RemoveRatingMutationOptionsParams) => ({
  mutationFn: async (movieId: number) => {
    await ratingApi.deleteRating(movieId);
  },
  onMutate: async (movieId: number): Promise<RemoveRatingContext> => {
    await queryClient.cancelQueries({ queryKey: ratingQueryKey });
    const previous =
      queryClient.getQueryData<InfiniteData<RatingLibraryResponse>>(
        ratingQueryKey,
      );

    queryClient.setQueryData<InfiniteData<RatingLibraryResponse>>(
      ratingQueryKey,
      (current) =>
        current ? removeMovieFromLibrary(current, movieId) : current,
    );

    return previous === undefined ? {} : { previous };
  },
  onError: (
    _error: unknown,
    _movieId: number,
    context: RemoveRatingContext | undefined,
  ) => {
    if (context?.previous) {
      queryClient.setQueryData(ratingQueryKey, context.previous);
    }
    onRemoveError();
  },
  onSettled: async () => {
    await queryClient.invalidateQueries({ queryKey: allRatingQueries });
  },
});
