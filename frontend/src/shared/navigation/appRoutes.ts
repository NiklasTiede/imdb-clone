import type { MovieSearchRequest } from "../../client/movies/generator-output";

export const movieDetailPath = (movieId: number): string => {
  if (!Number.isSafeInteger(movieId) || movieId <= 0) {
    throw new Error("A movie route requires a positive catalog ID.");
  }
  return `/movie?id=${movieId}`;
};

/** Build a fresh search route so a new agent request clears old filters and pagination. */
export const movieSearchPath = (
  query: string,
  filters: MovieSearchRequest,
): string => {
  const params = new URLSearchParams();
  if (query.trim()) params.set("query", query.trim());
  for (const genre of filters.movieGenre ?? []) params.append("genre", genre);
  if (filters.movieType) params.set("movieType", filters.movieType);
  for (const [key, value] of Object.entries({
    minYear: filters.minStartYear,
    maxYear: filters.maxStartYear,
    minRuntime: filters.minRuntimeMinutes,
    maxRuntime: filters.maxRuntimeMinutes,
  })) {
    if (value !== undefined && Number.isFinite(value))
      params.set(key, String(value));
  }
  return `/movie-search${params.size ? `?${params}` : ""}`;
};
