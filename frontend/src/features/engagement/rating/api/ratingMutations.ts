import type { QueryClient } from "@tanstack/react-query";
import { ratingApi } from "../../../../shared/api/moviesApi";

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
        queryKey: ["rating", "current-user"],
      }),
      queryClient?.invalidateQueries({
        queryKey: ["catalog", "movie", variables.movieId],
      }),
    ]);
  },
});
