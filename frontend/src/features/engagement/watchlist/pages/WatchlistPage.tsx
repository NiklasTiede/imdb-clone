import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import React, { useMemo, useState } from "react";
import { i18n } from "../../../../i18n";
import { useLocalStorageState } from "../../../../shared/hooks/useLocalStorageState";
import PageContent from "../../../../shared/layout/PageContent";
import { getUsername } from "../../../../shared/auth";
import EmptyWatchlist from "../components/EmptyWatchlist";
import WatchlistGrid from "../components/WatchlistGrid";
import WatchlistHeader from "../components/WatchlistHeader";
import WatchlistList from "../components/WatchlistList";
import WatchlistDecisionPanel from "../components/WatchlistDecisionPanel";
import WatchlistToolbar, {
  WatchlistView,
} from "../components/WatchlistToolbar";
import { removeFromWatchlistMutationOptions } from "../api/watchlistMutations";
import { watchlistQueries } from "../api/watchlistQueries";
import { WatchlistSort } from "../utils/watchlistSorting";
import { useSnackbar } from "notistack";

const WATCHLIST_PAGE_SIZE = 30;

const WatchlistPage = () => {
  const username = getUsername();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [sortBy, setSortBy] = useState<WatchlistSort>("addedAt_desc");
  const [view, setView] = useLocalStorageState<WatchlistView>(
    "watchlist.view",
    "grid",
    ["grid", "list"],
  );
  const watchlistQuery = watchlistQueries.library({
    size: WATCHLIST_PAGE_SIZE,
    sort: sortBy,
    username,
  });
  const { data, fetchNextPage, hasNextPage, isError, isFetchingNextPage, isLoading } =
    useInfiniteQuery(watchlistQuery);
  const items = useMemo(
    () =>
      (data?.pages ?? [])
        .flatMap((page) => page.items?.content ?? [])
        .filter((item, index, all) =>
          item.movieId === undefined
            ? true
            : all.findIndex((candidate) => candidate.movieId === item.movieId) === index,
        ),
    [data?.pages],
  );
  const insights = data?.pages[0]?.insights;

  const removeMutation = useMutation({
    ...removeFromWatchlistMutationOptions({
      onRemoveError: () => {
        enqueueSnackbar("Could not remove movie from watchlist", {
          variant: "error",
        });
      },
      queryClient,
      watchlistQueryKey: watchlistQuery.queryKey,
    }),
  });

  return (
    <PageContent maxWidth="1320px">
      <Stack spacing={2.5}>
        <WatchlistHeader />

        {isLoading && <CircularProgress aria-label="Loading watchlist" />}

        {isError && (
          <Alert severity="error">{i18n.watchlist.loadingError}</Alert>
        )}

        {!isLoading && !isError && items.length === 0 && <EmptyWatchlist />}

        {items.length > 0 && (
          <>
            <WatchlistDecisionPanel insights={insights} />
            <WatchlistToolbar
              sortBy={sortBy}
              view={view}
              onSortChange={setSortBy}
              onViewChange={setView}
            />
            {view === "grid" ? (
              <WatchlistGrid
                items={items}
                onRemove={(movieId) => removeMutation.mutate(movieId)}
              />
            ) : (
              <WatchlistList
                items={items}
                onRemove={(movieId) => removeMutation.mutate(movieId)}
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
                    : `Load more (${items.length} of ${insights?.totalMovies ?? items.length})`}
                </Button>
              </Stack>
            )}
          </>
        )}
      </Stack>
    </PageContent>
  );
};

export default WatchlistPage;
