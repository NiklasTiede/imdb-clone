import {
  readStreamingCountry,
  saveStreamingCountry,
} from "../model/streamingCountry";
import { pageContextFromLocation } from "../model/pageContext";
import { Box, Button, Snackbar } from "@mui/material";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  rateMovieMutationOptions,
  toggleWatchlistMutationOptions,
  watchlistQueryKeys,
} from "../../engagement";
import {
  lazy,
  Suspense,
  useMemo,
  useCallback,
  useState,
  useEffect,
} from "react";
import { useNavigate, useLocation } from "react-router";
import { useAuthSessionSnapshot } from "../../../shared/auth";
import { applicationDestination } from "../model/applicationNavigation";
import { useConciergeVoice } from "../hooks/useConciergeVoice";
import { ConciergeVoiceDock } from "./ConciergeVoicePanel";
import type { ApplicationAction } from "../model/concierge";

const ConciergeDebug = lazy(() => import("./ConciergeDebug"));

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
  const { search } = useLocation();
  // Opt in through the initial URL; retain debug access across agent navigation.
  const [debug] = useState(
    () => new URLSearchParams(search).get("conciergeDebug") === "1",
  );
  const { bootstrapped, session } = useAuthSessionSnapshot();
  if (!bootstrapped) {
    return null;
  }
  const identity = session?.id ?? null;
  return (
    <IdentityScopedConcierge
      key={identity ?? "anonymous"}
      accountId={identity}
      debug={debug}
    />
  );
};

const IdentityScopedConcierge = ({
  accountId,
  debug,
}: {
  accountId: number | null;
  debug: boolean;
}) => {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(debug);
  const [dismissedNotice, setDismissedNotice] = useState<string | null>(null);
  const [streamingCountry, setStreamingCountry] = useState(() =>
    readStreamingCountry(accountId),
  );
  const changeStreamingCountry = useCallback(
    (country: string) => {
      setStreamingCountry(country);
      saveStreamingCountry(accountId, country);
    },
    [accountId],
  );
  const [receipt, setReceipt] = useState<PersonalReceipt | null>(null);
  const undoWatchlist = useMutation(
    toggleWatchlistMutationOptions(queryClient),
  );
  const undoRating = useMutation(rateMovieMutationOptions(queryClient));
  const resetWatchlistUndo = undoWatchlist.reset;
  const resetRatingUndo = undoRating.reset;
  const navigate = useNavigate();
  const { pathname, search, hash } = useLocation();
  const pageContext = useMemo(
    () => ({
      ...pageContextFromLocation({ pathname, search, hash }),
      streamingCountry,
    }),
    [pathname, search, hash, streamingCountry],
  );
  const handleApplicationAction = useCallback(
    (action: ApplicationAction) => {
      if (action.type === "open_watchlist" || action.type === "open_ratings") {
        if (accountId === null) throw new Error("Sign in required");
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
      const destination = applicationDestination(action, accountId !== null);
      setOpen(false);
      void navigate(destination);
      return destination;
    },
    [navigate, accountId, queryClient, resetWatchlistUndo, resetRatingUndo],
  );

  const voice = useConciergeVoice(handleApplicationAction, pageContext);
  const { confirmNavigation: confirmVoiceNavigation, turns: voiceTurns } =
    voice;
  useEffect(() => {
    const currentPath = `${pathname}${search}${hash}`;
    confirmVoiceNavigation(currentPath);
  }, [pathname, search, hash, voiceTurns, confirmVoiceNavigation]);
  useEffect(() => {
    if (debug && (voice.status === "error" || voice.notice)) setOpen(true);
  }, [debug, voice.status, voice.notice]);
  const voiceNotice = voice.error ?? voice.notice;
  useEffect(() => {
    if (!voiceNotice) setDismissedNotice(null);
  }, [voiceNotice]);

  return (
    <>
      <Box
        aria-hidden="true"
        sx={{ height: "calc(104px + env(safe-area-inset-bottom))" }}
      />
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
      {!debug && (
        <Snackbar
          open={!!voiceNotice && dismissedNotice !== voiceNotice}
          message={voiceNotice}
          autoHideDuration={10000}
          onClose={(_, reason) => {
            if (reason !== "clickaway") setDismissedNotice(voiceNotice);
          }}
          sx={{ bottom: { xs: 140, sm: 140 } }}
        />
      )}
      {debug ? (
        <Suspense fallback={null}>
          <ConciergeDebug
            accountId={accountId}
            voice={voice}
            pageContext={pageContext}
            onAction={handleApplicationAction}
            open={open}
            onClose={() => setOpen(false)}
            onToggle={() => setOpen((current) => !current)}
            streamingCountry={streamingCountry}
            onStreamingCountryChange={changeStreamingCountry}
          />
        </Suspense>
      ) : (
        <ConciergeVoiceDock
          voice={voice}
          conversationOpen={false}
          toggleConversation={() => undefined}
        />
      )}
    </>
  );
};

export default ConciergeExperience;
