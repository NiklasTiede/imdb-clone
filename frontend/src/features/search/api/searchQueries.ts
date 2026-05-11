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

export const searchQueries = {
  movies: ({ filters, page, query, size }: MovieSearchQueryParams) => {
    const normalizedQuery = query?.trim() || null;
    const hasFilters = Object.keys(filters).length > 0;

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
      queryKey: ["search", "movies", normalizedQuery, filters, page, size],
    };
  },
};
