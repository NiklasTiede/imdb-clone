import { recommendationApi } from "../../../shared/api/moviesApi";

export const recommendationQueries = {
  similarMovies: (movieId: number | undefined) => ({
    enabled: movieId !== undefined,
    queryFn: async () => {
      if (movieId === undefined) {
        throw new Error("Movie id is required.");
      }
      const response = await recommendationApi.similarMovies(movieId, 15);
      return (response.data.items ?? [])
        .map((item) => item.movie)
        .filter((movie) => movie !== undefined);
    },
    queryKey: ["catalog", "movie", movieId, "similar"] as const,
  }),
};
