import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import { alpha } from "@mui/material/styles";
import { Button, Fab, Snackbar, useMediaQuery, useTheme } from "@mui/material";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  rateMovieMutationOptions,
  toggleWatchlistMutationOptions,
  watchlistQueryKeys,
} from "../../engagement";
import { useCallback, useState } from "react";
import { useNavigate } from "react-router";
import { useAuthSessionSnapshot } from "../../../shared/auth";
import { movieDetailPath } from "../../../shared/navigation/appRoutes";
import { movieColors } from "../../../theme";
import { getConciergeClientId } from "../model/browserIdentity";
import ConciergeDrawer from "./ConciergeDrawer";
import { useConciergeVoice } from "../hooks/useConciergeVoice";
import { ConciergeVoiceDock } from "./ConciergeVoicePanel";
import type { ApplicationAction } from "../model/concierge";

type PersonalReceipt = {
  operationId: string;
  movieId: number;
  changed: boolean;
  message: string;
} & (
  | { kind: "watchlist"; isBookmarked: boolean }
  | {
      kind: "rating";
      previousScore: number | null;
    }
);

const ConciergeExperience = () => {
  const { bootstrapped, session } = useAuthSessionSnapshot();
  if (!bootstrapped) {
    return null;
  }
  const identity = session?.id ?? null;
  return (
    <IdentityScopedConcierge
      key={identity ?? "anonymous"}
      accountId={identity}
    />
  );
};

const IdentityScopedConcierge = ({
  accountId,
}: {
  accountId: number | null;
}) => {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [receipt, setReceipt] = useState<PersonalReceipt | null>(null);
  const undoWatchlist = useMutation(
    toggleWatchlistMutationOptions(queryClient),
  );
  const undoRating = useMutation(rateMovieMutationOptions(queryClient));
  const resetWatchlistUndo = undoWatchlist.reset;
  const resetRatingUndo = undoRating.reset;
  const navigate = useNavigate();
  const theme = useTheme();
  const mobile = useMediaQuery(theme.breakpoints.down("sm"));
  const clientId = getConciergeClientId(accountId);
  const handleApplicationAction = useCallback(
    (action: ApplicationAction) => {
      if (action.type === "open_watchlist" || action.type === "open_ratings") {
        if (accountId === null) return;
        void queryClient.invalidateQueries({
          queryKey:
            action.type === "open_watchlist"
              ? watchlistQueryKeys.all
              : ["rating"],
        });
        if (action.movieId) {
          void queryClient.invalidateQueries({
            queryKey: ["catalog", "movie", action.movieId],
          });
        }
        resetWatchlistUndo();
        resetRatingUndo();
        if (action.type === "open_ratings") {
          setReceipt({
            kind: "rating",
            operationId: action.operationId,
            movieId: action.movieId,
            changed: action.changed,
            previousScore: action.previousScore,
            message:
              action.score === null
                ? action.changed
                  ? "Your rating was removed."
                  : "You have no rating for this movie."
                : action.changed
                  ? `Your rating was saved: ${action.score}/10.`
                  : `Your rating is already ${action.score}/10.`,
          });
        } else if (action.operationId && action.movieId) {
          const removal =
            action.removed !== undefined && action.removed !== null;
          setReceipt({
            kind: "watchlist",
            operationId: action.operationId,
            movieId: action.movieId,
            changed: (removal ? action.removed : action.created) === true,
            isBookmarked: !removal,
            message: removal
              ? action.removed
                ? "Movie removed from your watchlist."
                : "Movie is not on your watchlist."
              : action.created
                ? "Movie added to your watchlist."
                : "Movie is already on your watchlist.",
          });
        }
      }
      const destination =
        action.type === "open_movie"
          ? movieDetailPath(action.movieId)
          : action.type === "open_watchlist"
            ? "/your-watchlist"
            : action.type === "open_ratings"
              ? "/your-ratings"
              : "/login";
      setOpen(false);
      void navigate(destination);
    },
    [navigate, accountId, queryClient, resetWatchlistUndo, resetRatingUndo],
  );

  const voice = useConciergeVoice(handleApplicationAction);

  return (
    <>
      <Snackbar
        open={receipt !== null}
        sx={
          voice.active
            ? {
                bottom: {
                  xs: "max(92px, calc(env(safe-area-inset-bottom) + 80px))",
                  sm: 24,
                },
              }
            : undefined
        }
        autoHideDuration={10000}
        onClose={() => setReceipt(null)}
        message={
          undoWatchlist.isError || undoRating.isError
            ? "Could not undo. Please try again."
            : receipt?.message
        }
        action={
          receipt?.changed ? (
            <Button
              color="inherit"
              disabled={undoWatchlist.isPending || undoRating.isPending}
              onClick={() => {
                const onSuccess = () =>
                  setReceipt((current) =>
                    current === receipt ? null : current,
                  );
                if (receipt.kind === "watchlist") {
                  undoWatchlist.mutate(
                    {
                      movieId: receipt.movieId,
                      isBookmarked: receipt.isBookmarked,
                    },
                    { onSuccess },
                  );
                } else {
                  undoRating.mutate(
                    { movieId: receipt.movieId, score: receipt.previousScore },
                    { onSuccess },
                  );
                }
              }}
            >
              Undo
            </Button>
          ) : undefined
        }
      />
      {voice.active ? (
        !open && (
          <ConciergeVoiceDock voice={voice} expand={() => setOpen(true)} />
        )
      ) : mobile ? (
        <Fab
          aria-label="Ask the Movie Concierge"
          onClick={() => setOpen(true)}
          size="medium"
          sx={{
            bgcolor: movieColors.brand,
            bottom: 18,
            boxShadow: "0 12px 32px rgba(0,0,0,0.42)",
            color: movieColors.brandInk,
            position: "fixed",
            right: 18,
            zIndex: theme.zIndex.fab,
            "&:hover": { bgcolor: "#ffe053" },
          }}
        >
          <AutoAwesomeRoundedIcon />
        </Fab>
      ) : (
        <Button
          aria-label="Ask the Movie Concierge"
          onClick={() => setOpen(true)}
          startIcon={<AutoAwesomeRoundedIcon sx={{ fontSize: 17 }} />}
          sx={{
            backdropFilter: "blur(12px)",
            bgcolor: alpha(movieColors.surfaceElevated, 0.94),
            border: `1px solid ${alpha(movieColors.brand, 0.35)}`,
            borderRadius: 10,
            bottom: 24,
            boxShadow: "0 14px 38px rgba(0,0,0,0.38)",
            color: "rgba(255,255,255,0.9)",
            fontSize: 11.5,
            fontWeight: 700,
            px: 2,
            py: 1.1,
            position: "fixed",
            right: 24,
            textTransform: "none",
            zIndex: theme.zIndex.fab,
            "& .MuiButton-startIcon": { color: movieColors.brand },
            "&:hover": {
              bgcolor: movieColors.surfaceElevated,
              borderColor: alpha(movieColors.brand, 0.68),
            },
          }}
        >
          Ask Concierge
        </Button>
      )}
      <ConciergeDrawer
        voice={voice}
        clientId={clientId}
        onClose={() => setOpen(false)}
        onUiAction={handleApplicationAction}
        open={open}
      />
    </>
  );
};

export default ConciergeExperience;
