import Alert from "@mui/material/Alert";
import LinearProgress from "@mui/material/LinearProgress";
import Stack from "@mui/material/Stack";
import React from "react";
import { useLocation } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { searchQueries } from "../api/searchQueries";
import SearchEmptyState from "../components/SearchEmptyState";
import SearchHeader from "../components/SearchHeader";
import SearchMovieGrid from "../components/SearchMovieGrid";
import { parseSearchUrlState } from "../utils/searchUrlState";
import PageContent from "../../../shared/layout/PageContent";

export const SEARCH_RESULTS_MAX_WIDTH_PX = 1320;

const MovieSearchPage = () => {
  const location = useLocation();
  const searchState = parseSearchUrlState(location.search);

  const { data, isError, isFetching } = useQuery(
    searchQueries.movies({
      filters: {},
      page: searchState.page,
      query: searchState.query,
      size: 24,
    }),
  );
  const movies = data?.content ?? [];
  const hasSearchQuery = searchState.query !== null;
  const showEmptyState = hasSearchQuery && !isFetching && movies.length === 0;

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

        {movies.length > 0 && <SearchMovieGrid movies={movies} />}
      </Stack>
    </PageContent>
  );
};

export default MovieSearchPage;
