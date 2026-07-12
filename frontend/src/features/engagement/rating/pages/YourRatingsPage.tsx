import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { i18n } from "../../../../i18n";
import { getUsername } from "../../../../shared/auth";
import { useLocalStorageState } from "../../../../shared/hooks/useLocalStorageState";
import PageContent from "../../../../shared/layout/PageContent";
import { rateMovieMutationOptions } from "../api/ratingMutations";
import { ratingQueries } from "../api/ratingQueries";
import EmptyRatings from "../components/EmptyRatings";
import RatingsGrid from "../components/RatingsGrid";
import RatingsHeader from "../components/RatingsHeader";
import RatingsList from "../components/RatingsList";
import RatingsTasteSnapshot from "../components/RatingsTasteSnapshot";
import RatingsToolbar, { type RatingsView } from "../components/RatingsToolbar";
import {
  allScoresRange,
  filterRatedMovies,
  type RatingSort,
  type ScoreRange,
} from "../utils/ratingsSorting";

const RATINGS_PAGE_SIZE = 30;

const YourRatingsPage = () => {
  const username = getUsername();
  const queryClient = useQueryClient();
  const [sortBy, setSortBy] = useState<RatingSort>("score_desc");
  const [scoreRange, setScoreRange] = useState<ScoreRange>(allScoresRange);
  const [view, setView] = useLocalStorageState<RatingsView>(
    "ratings.view",
    "grid",
    ["grid", "list"],
  );
  const ratingsQuery = ratingQueries.library({
    size: RATINGS_PAGE_SIZE,
    sort: sortBy,
    username,
  });
  const { data, fetchNextPage, hasNextPage, isError, isLoading, isFetchingNextPage } =
    useInfiniteQuery(ratingsQuery);
  const ratedMovies = useMemo(
    () =>
      (data?.pages ?? [])
        .flatMap((page) => page.items?.content ?? [])
        .flatMap((item) =>
          item.movie && item.rating !== undefined
            ? [{ movie: item.movie, rating: item.rating }]
            : [],
        ),
    [data?.pages],
  );
  const visibleMovies = useMemo(
    () => filterRatedMovies(ratedMovies, scoreRange),
    [ratedMovies, scoreRange],
  );
  const insights = data?.pages[0]?.insights;

  const removeRating = useMutation({
    ...rateMovieMutationOptions(queryClient),
  });

  return (
    <PageContent maxWidth="1320px">
      <Stack spacing={2.5}>
        <RatingsHeader />

        {isLoading && <CircularProgress aria-label="Loading ratings" />}

        {isError && <Alert severity="error">{i18n.ratings.loadingError}</Alert>}

        {!isLoading && !isError && ratedMovies.length === 0 && <EmptyRatings />}

        {ratedMovies.length > 0 && (
          <>
            <RatingsTasteSnapshot insights={insights} />
            <RatingsToolbar
              scoreRange={scoreRange}
              sortBy={sortBy}
              view={view}
              onScoreRangeChange={setScoreRange}
              onSortChange={setSortBy}
              onViewChange={setView}
            />
            {visibleMovies.length === 0 ? (
              <Alert
                action={
                  <Button
                    color="inherit"
                    onClick={() => setScoreRange(allScoresRange)}
                    size="small"
                    type="button"
                  >
                    Clear filter
                  </Button>
                }
                severity="info"
              >
                No ratings in this range.
              </Alert>
            ) : view === "grid" ? (
              <RatingsGrid
                items={visibleMovies}
                onRemove={(movieId) =>
                  removeRating.mutate({ movieId, score: null })
                }
              />
            ) : (
              <RatingsList
                items={visibleMovies}
                onRemove={(movieId) =>
                  removeRating.mutate({ movieId, score: null })
                }
              />
            )}
            {hasNextPage && (
              <Stack sx={{ alignItems: "center", pt: 1 }}>
                <Button
                  disabled={isFetchingNextPage}
                  onClick={() => void fetchNextPage()}
                  variant="outlined"
                >
                  {isFetchingNextPage
                    ? "Loading more…"
                    : `Load more (${ratedMovies.length} of ${insights?.totalRatings ?? ratedMovies.length})`}
                </Button>
              </Stack>
            )}
          </>
        )}
      </Stack>
    </PageContent>
  );
};

export default YourRatingsPage;
