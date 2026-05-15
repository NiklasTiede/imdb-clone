import Alert from "@mui/material/Alert";
import LinearProgress from "@mui/material/LinearProgress";
import Stack from "@mui/material/Stack";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import ViewListIcon from "@mui/icons-material/ViewListSharp";
import ViewModuleIcon from "@mui/icons-material/ViewModuleSharp";
import { useLocation } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { searchQueries } from "../api/searchQueries";
import SearchEmptyState from "../components/SearchEmptyState";
import SearchHeader from "../components/SearchHeader";
import SearchMovieGrid from "../components/SearchMovieGrid";
import SearchMovieList from "../components/SearchMovieList";
import { parseSearchUrlState } from "../utils/searchUrlState";
import { useLocalStorageState } from "../../../shared/hooks/useLocalStorageState";
import PageContent from "../../../shared/layout/PageContent";
import type { Movie } from "../../catalog";
import type { SearchUrlState } from "../utils/searchUrlState";

export const SEARCH_RESULTS_MAX_WIDTH_PX = 1320;
export type SearchView = "grid" | "list";
export const SEARCH_VIEW_STORAGE_KEY = "search.view";

const MovieSearchPage = () => {
  const location = useLocation();
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
  const movies = sortSearchMovies(data?.content ?? [], searchState.sort);
  const hasSearchCriteria =
    searchState.query !== null || Object.keys(searchState.filters).length > 0;
  const showEmptyState = shouldShowSearchEmptyState({
    hasSearchCriteria,
    isError,
    isFetching,
    movieCount: movies.length,
  });

  return (
    <PageContent maxWidth={`${SEARCH_RESULTS_MAX_WIDTH_PX}px`}>
      <Stack spacing={2.5}>
        <SearchHeader
          query={searchState.query}
          totalCount={data?.totalElements}
        />

        {isFetching && <LinearProgress aria-label="Loading search results" />}

        {isError && (
          <Alert severity="error">
            Error while attempting to load search results.
          </Alert>
        )}

        {showEmptyState && <SearchEmptyState />}

        {movies.length > 0 && (
          <>
            <Stack direction="row" sx={{ justifyContent: "flex-end" }}>
              <ToggleButtonGroup
                exclusive
                onChange={(_, nextView: SearchView | null) => {
                  if (nextView) {
                    setView(nextView);
                  }
                }}
                size="small"
                value={view}
              >
                <ToggleButton aria-label="Grid view" value="grid">
                  <ViewModuleIcon fontSize="small" />
                </ToggleButton>
                <ToggleButton aria-label="List view" value="list">
                  <ViewListIcon fontSize="small" />
                </ToggleButton>
              </ToggleButtonGroup>
            </Stack>
            {view === "grid" ? (
              <SearchMovieGrid movies={movies} />
            ) : (
              <SearchMovieList movies={movies} />
            )}
          </>
        )}
      </Stack>
    </PageContent>
  );
};

export const sortSearchMovies = (
  movies: Movie[],
  sort: SearchUrlState["sort"],
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

export default MovieSearchPage;
