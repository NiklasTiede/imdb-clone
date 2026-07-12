import Alert from "@mui/material/Alert";
import Stack from "@mui/material/Stack";
import { useLocation, useNavigate } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { searchQueries } from "../api/searchQueries";
import SearchEmptyState from "../components/SearchEmptyState";
import SearchFilterBar from "../components/SearchFilterBar";
import SearchHeader from "../components/SearchHeader";
import SearchLoadingSlot from "../components/SearchLoadingSlot";
import SearchMovieGrid from "../components/SearchMovieGrid";
import SearchMovieList from "../components/SearchMovieList";
import SearchResultsPagination from "../components/SearchResultsPagination";
import { createSearchUrl, parseSearchUrlState } from "../utils/searchUrlState";
import { useLocalStorageState } from "../../../shared/hooks/useLocalStorageState";
import PageContent from "../../../shared/layout/PageContent";
import type { SearchUrlPatch } from "../utils/searchUrlState";
import {
  SEARCH_RESULTS_MAX_WIDTH_PX,
  SEARCH_VIEW_STORAGE_KEY,
  shouldShowSearchEmptyState,
  type SearchView,
} from "./MovieSearchPage.utils";

const MovieSearchPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const searchState = parseSearchUrlState(location.search);
  const [view, setView] = useLocalStorageState<SearchView>(
    SEARCH_VIEW_STORAGE_KEY,
    "grid",
    ["grid", "list"],
  );

  const { data, isError, isFetching } = useQuery(
    searchQueries.movies({
      filters: searchState.filters,
      page: searchState.page,
      query: searchState.query,
      size: 24,
    }),
  );
  const movies = data?.content ?? [];
  const hasSearchCriteria =
    searchState.query !== null || Object.keys(searchState.filters).length > 0;
  const showEmptyState = shouldShowSearchEmptyState({
    hasSearchCriteria,
    isError,
    isFetching,
    movieCount: movies.length,
  });
  const updateSearchUrl = (patch: SearchUrlPatch) => {
    void navigate({
      pathname: location.pathname,
      search: createSearchUrl(location.search, patch),
    });
  };
  const clearFilters = () => {
    updateSearchUrl({
      genre: null,
      maxRuntime: null,
      maxYear: null,
      minRuntime: null,
      minYear: null,
    });
  };
  const hasActiveFilters = Object.keys(searchState.filters).length > 0;

  return (
    <PageContent maxWidth={`${SEARCH_RESULTS_MAX_WIDTH_PX}px`}>
      <Stack spacing={2.5}>
        <SearchHeader
          onViewChange={setView}
          query={searchState.query}
          totalCount={data?.totalElements}
          view={view}
        />

        <SearchFilterBar
          filters={searchState.filters}
          onChange={updateSearchUrl}
          onClear={clearFilters}
        />

        <SearchLoadingSlot loading={isFetching} />

        {isError && (
          <Alert severity="error">
            Error while attempting to load search results.
          </Alert>
        )}

        {showEmptyState && (
          <SearchEmptyState
            onClearFilters={hasActiveFilters ? clearFilters : undefined}
          />
        )}

        {movies.length > 0 && (
          <>
            {view === "grid" ? (
              <SearchMovieGrid movies={movies} />
            ) : (
              <SearchMovieList movies={movies} />
            )}
            <SearchResultsPagination
              onPageChange={(page) => updateSearchUrl({ page })}
              page={searchState.page}
              pageCount={data?.totalPages}
            />
          </>
        )}
      </Stack>
    </PageContent>
  );
};

export default MovieSearchPage;
