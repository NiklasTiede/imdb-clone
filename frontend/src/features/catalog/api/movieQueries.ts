import { moviesApi } from "../../../shared/api/moviesApi";

export const movieQueries = {
  detail: (movieId: number | null) => ({
    enabled: movieId !== null,
    queryFn: async () => {
      if (movieId === null) {
        throw new Error("Movie id is required.");
      }
      const response = await moviesApi.getMovieById(movieId);
      return response.data;
    },
    queryKey: ["catalog", "movie", movieId] as const,
  }),
};
