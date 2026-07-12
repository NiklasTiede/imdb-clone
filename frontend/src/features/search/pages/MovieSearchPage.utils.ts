export const SEARCH_RESULTS_MAX_WIDTH_PX = 1320;
export type SearchView = "grid" | "list";
export const SEARCH_VIEW_STORAGE_KEY = "search.view";

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
