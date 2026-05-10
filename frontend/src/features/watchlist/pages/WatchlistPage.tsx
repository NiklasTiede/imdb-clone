import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import React, { useMemo, useState } from "react";
import {
  PagedResponseWatchedMovieRecord,
  WatchedMovieRecord,
} from "../../../client/movies/generator-output";
import { i18n } from "../../../i18n";
import { useLocalStorageState } from "../../../shared/hooks/useLocalStorageState";
import PageContent from "../../../shared/layout/PageContent";
import { getUsername } from "../../../utils/jwtHelper";
import EmptyWatchlist from "../components/EmptyWatchlist";
import PickForMeDialog from "../components/PickForMeDialog";
import WatchlistGrid from "../components/WatchlistGrid";
import WatchlistHeader from "../components/WatchlistHeader";
import WatchlistList from "../components/WatchlistList";
import WatchlistStats from "../components/WatchlistStats";
import WatchlistToolbar, {
  WatchlistView,
} from "../components/WatchlistToolbar";
import { addToWatchlist, removeFromWatchlist } from "../api/watchlistMutations";
import { watchlistQueries } from "../api/watchlistQueries";
import { pickRandomWatchlistItem } from "../utils/pickRandomWatchlistItem";
import {
  sortWatchlistItems,
  WatchlistSort,
} from "../utils/watchlistSorting";
import { useSnackbar } from "notistack";

const WATCHLIST_PAGE_SIZE = 30;

const WatchlistPage = () => {
  const username = getUsername();
  const queryClient = useQueryClient();
  const { closeSnackbar, enqueueSnackbar } = useSnackbar();
  const [sortBy, setSortBy] = useState<WatchlistSort>("addedAt_desc");
  const [view, setView] = useLocalStorageState<WatchlistView>(
    "watchlist.view",
    "grid",
    ["grid", "list"],
  );
  const [pickedMovie, setPickedMovie] = useState<WatchedMovieRecord | null>(
    null,
  );
  const [pickDialogOpen, setPickDialogOpen] = useState(false);
  const watchlistQuery = watchlistQueries.currentUserItems({
    page: 0,
    size: WATCHLIST_PAGE_SIZE,
    username,
  });
  const { data, isError, isLoading } = useQuery(
    watchlistQuery,
  );
  const items = useMemo(() => data?.content ?? [], [data?.content]);
  const sortedItems = useMemo(
    () => sortWatchlistItems(items, sortBy),
    [items, sortBy],
  );

  const addMutation = useMutation({
    mutationFn: addToWatchlist,
    onError: () => {
      enqueueSnackbar("Could not restore movie to watchlist", {
        variant: "error",
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist"] });
    },
  });

  const removeMutation = useMutation({
    mutationFn: removeFromWatchlist,
    onMutate: async (movieId: number) => {
      await queryClient.cancelQueries({ queryKey: watchlistQuery.queryKey });
      const previous =
        queryClient.getQueryData<PagedResponseWatchedMovieRecord>(
          watchlistQuery.queryKey,
        );
      queryClient.setQueryData<PagedResponseWatchedMovieRecord>(
        watchlistQuery.queryKey,
        (current) =>
          current
            ? {
                ...current,
                content: current.content?.filter(
                  (item) => (item.movieId ?? item.movie?.id) !== movieId,
                ),
                totalElements: Math.max((current.totalElements ?? 1) - 1, 0),
              }
            : current,
      );
      return { previous };
    },
    onError: (_error, _movieId, context) => {
      if (context?.previous) {
        queryClient.setQueryData(watchlistQuery.queryKey, context.previous);
      }
      enqueueSnackbar("Could not remove movie from watchlist", {
        variant: "error",
      });
    },
    onSuccess: (_data, movieId) => {
      enqueueSnackbar("Removed from watchlist", {
        action: (snackbarId) => (
          <Button
            color="inherit"
            onClick={() => {
              addMutation.mutate(movieId);
              closeSnackbar(snackbarId);
            }}
            size="small"
          >
            Undo
          </Button>
        ),
        variant: "info",
      });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist"] });
    },
  });

  const pickForMe = () => {
    const nextMovie = pickRandomWatchlistItem(
      sortedItems,
      pickedMovie?.movieId,
    );
    setPickedMovie(nextMovie);
    setPickDialogOpen(nextMovie !== null);
  };

  return (
    <PageContent maxWidth="1320px">
      <Stack spacing={2.5}>
        <WatchlistHeader
          disabled={items.length === 0}
          onPickForMe={pickForMe}
        />

        {isLoading && <CircularProgress aria-label="Loading watchlist" />}

        {isError && (
          <Alert severity="error">{i18n.watchlist.loadingError}</Alert>
        )}

        {!isLoading && !isError && items.length === 0 && <EmptyWatchlist />}

        {items.length > 0 && (
          <>
            <WatchlistStats items={items} />
            <WatchlistToolbar
              sortBy={sortBy}
              view={view}
              onSortChange={setSortBy}
              onViewChange={setView}
            />
            {view === "grid" ? (
              <WatchlistGrid
                items={sortedItems}
                onRemove={(movieId) => removeMutation.mutate(movieId)}
              />
            ) : (
              <WatchlistList
                items={sortedItems}
                onRemove={(movieId) => removeMutation.mutate(movieId)}
              />
            )}
          </>
        )}
      </Stack>
      <PickForMeDialog
        canPickAnother={items.length > 1}
        movie={pickedMovie}
        onClose={() => setPickDialogOpen(false)}
        onPickAnother={pickForMe}
        open={pickDialogOpen}
      />
    </PageContent>
  );
};

export default WatchlistPage;
