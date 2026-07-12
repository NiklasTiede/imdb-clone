import {
  MovieSearchRequest,
  PagedResponseMovieRecord,
} from "../../../client/movies/generator-output";
import { searchApi } from "../../../shared/api/moviesApi";

export type MovieSearchQueryParams = {
  filters: MovieSearchRequest;
  page: number;
  query: string | null;
  size: number;
};

export const normalizeSearchFiltersForKey = (
  filters: MovieSearchRequest,
) => ({
  maxRuntimeMinutes: filters.maxRuntimeMinutes ?? null,
  maxStartYear: filters.maxStartYear ?? null,
  minRuntimeMinutes: filters.minRuntimeMinutes ?? null,
  minStartYear: filters.minStartYear ?? null,
  movieGenre: Array.from(filters.movieGenre ?? []).sort(),
  movieType: filters.movieType ?? null,
});

export const searchQueries = {
  movies: ({ filters, page, query, size }: MovieSearchQueryParams) => {
    const normalizedQuery = query?.trim() || null;
    const hasFilters = Object.keys(filters).length > 0;
    const filterKey = normalizeSearchFiltersForKey(filters);

    return {
      enabled: normalizedQuery !== null || hasFilters,
      queryFn: async (): Promise<PagedResponseMovieRecord> => {
        if (normalizedQuery === null && !hasFilters) {
          throw new Error("Search query or filters are required.");
        }
        const response = await searchApi.search(
          normalizedQuery ?? "",
          filters,
          page,
          size,
        );
        return response.data;
      },
      queryKey: ["search", "movies", normalizedQuery, filterKey, page, size],
    };
  },
};
