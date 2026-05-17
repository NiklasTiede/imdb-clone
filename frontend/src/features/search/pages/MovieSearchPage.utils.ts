import type { Movie } from "../../catalog";
import type { SearchSort } from "../utils/searchUrlState";

export const SEARCH_RESULTS_MAX_WIDTH_PX = 1320;
export type SearchView = "grid" | "list";
export const SEARCH_VIEW_STORAGE_KEY = "search.view";

export const sortSearchMovies = (
  movies: Movie[],
  sort: SearchSort,
): Movie[] => {
  if (sort !== "rating_desc") {
    return movies;
  }

  return [...movies].sort(
    (left, right) => (right.imdbRating ?? 0) - (left.imdbRating ?? 0),
  );
};

export const shouldShowSearchEmptyState = ({
  hasSearchCriteria,
  isError,
  isFetching,
  movieCount,
}: {
  hasSearchCriteria: boolean;
  isError: boolean;
  isFetching: boolean;
  movieCount: number;
}): boolean => hasSearchCriteria && !isError && !isFetching && movieCount === 0;
