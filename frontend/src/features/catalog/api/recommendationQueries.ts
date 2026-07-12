import { recommendationApi } from "../../../shared/api/moviesApi";
import type { Movie } from "../model/movie";

export type SimilarMovie = {
  explanation: string;
  movie: Movie;
  reason?: string;
};

export const recommendationQueries = {
  similarMovies: (movieId: number | undefined) => ({
    enabled: movieId !== undefined,
    queryFn: async () => {
      if (movieId === undefined) {
        throw new Error("Movie id is required.");
      }
      const response = await recommendationApi.similarMovies(movieId, 15);
      return (response.data.items ?? []).flatMap((item) =>
        item.movie
          ? [{
              explanation: item.explanation ?? "Chosen for shared movie DNA.",
              movie: item.movie as Movie,
              reason: item.reason,
            }]
          : [],
      );
    },
    queryKey: ["catalog", "movie", movieId, "similar"] as const,
  }),
};
