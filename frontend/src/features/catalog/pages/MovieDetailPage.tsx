import MovieIcon from "@mui/icons-material/Movie";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Skeleton from "@mui/material/Skeleton";
import Snackbar from "@mui/material/Snackbar";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link as RouterLink, useLocation, useNavigate } from "react-router";
import { authSession, useAuthSession } from "../../../shared/auth";
import PageContent from "../../../shared/layout/PageContent";
import StatusState from "../../../shared/layout/StatusState";
import {
  rateMovieMutationOptions,
  ratingQueries,
  toggleWatchlistMutationOptions,
  watchlistQueries,
} from "../../engagement";
import { movieQueries } from "../api/movieQueries";
import { MovieHero } from "../components/MovieHero";
import MovieRatingDialog from "../components/MovieRatingDialog";
import Synopsis from "../components/Synopsis";
import { shareMovie } from "../utils/shareMovie";

type PageFeedback = {
  message: string;
  severity: "error" | "success";
};

const MovieDetailPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const queryParams = new URLSearchParams(location.search);
  const movieId = parseMovieId(queryParams.get("id"));
  const isAuthenticated = useAuthSession();
  const username = isAuthenticated ? authSession.getUsername() : null;
  const queryClient = useQueryClient();
  const [authPrompt, setAuthPrompt] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<PageFeedback | null>(null);
  const [isRatingOpen, setIsRatingOpen] = useState(false);
  const [ratingError, setRatingError] = useState<string | null>(null);

  const movieQuery = useQuery(movieQueries.detail(movieId));
  const movie = movieQuery.data;

  const { data: watchedMovieIds } = useQuery(
    watchlistQueries.movieIds({ username }),
  );
  const { data: userRating } = useQuery(
    ratingQueries.userRatingForMovie({ movieId, username }),
  );

  const toggleWatchlist = useMutation({
    ...toggleWatchlistMutationOptions(queryClient),
    onError: () =>
      setFeedback({
        message: "Could not update your watchlist. Please try again.",
        severity: "error",
      }),
  });

  const rateMovie = useMutation({
    ...rateMovieMutationOptions(queryClient),
    onMutate: () => setRatingError(null),
    onError: () =>
      setRatingError("Could not save your rating. Please try again."),
  });

  const navigateToLogin = () => {
    navigate("/login", { state: { from: location } });
  };

  if (movieId === null) {
    return (
      <PageContent maxWidth="1280px" sx={{ pt: { xs: 4, sm: 7 } }}>
        <StatusState
          icon={<MovieIcon />}
          title="Choose a movie"
          action={
            <Button component={RouterLink} to="/" variant="contained">
              Browse movies
            </Button>
          }
        >
          This movie link is incomplete or invalid.
        </StatusState>
      </PageContent>
    );
  }

  if (movieQuery.isPending) {
    return <MovieDetailSkeleton />;
  }

  if (movieQuery.isError || !movie) {
    return (
      <PageContent maxWidth="1280px" sx={{ pt: { xs: 4, sm: 7 } }}>
        <StatusState
          icon={<MovieIcon />}
          title="Movie unavailable"
          action={
            <Button variant="contained" onClick={() => movieQuery.refetch()}>
              Try again
            </Button>
          }
        >
          We could not load this movie. It may no longer be available.
        </StatusState>
      </PageContent>
    );
  }

  const isBookmarked = Boolean(
    movie.id !== undefined && watchedMovieIds?.has(movie.id),
  );

  const requestAuthentication = (action: "rate" | "watchlist") => {
    setAuthPrompt(
      action === "rate"
        ? "Sign in to rate this movie."
        : "Sign in to add this movie to your watchlist.",
    );
  };

  const handleToggleBookmark = () => {
    if (!username || movie.id === undefined) {
      requestAuthentication("watchlist");
      return;
    }
    setAuthPrompt(null);
    toggleWatchlist.mutate({ movieId: movie.id, isBookmarked });
  };

  const handleOpenRating = () => {
    if (!username || movie.id === undefined) {
      requestAuthentication("rate");
      return;
    }
    setAuthPrompt(null);
    setRatingError(null);
    setIsRatingOpen(true);
  };

  const handleRate = (score: number | null) => {
    if (!username || movie.id === undefined) {
      setIsRatingOpen(false);
      requestAuthentication("rate");
      return;
    }

    rateMovie.mutate(
      { movieId: movie.id, score },
      {
        onSuccess: () => {
          setIsRatingOpen(false);
          setFeedback({
            message: score === null ? "Rating removed." : "Rating saved.",
            severity: "success",
          });
        },
      },
    );
  };

  const handleShare = async () => {
    if (movie.id === undefined) {
      return;
    }

    try {
      const result = await shareMovie({
        movieId: movie.id,
        title: movie.primaryTitle?.trim() || "Movie",
      });

      if (result !== "cancelled") {
        setFeedback({
          message:
            result === "copied" ? "Movie link copied." : "Movie shared.",
          severity: "success",
        });
      }
    } catch {
      setFeedback({
        message: "Could not share this movie. Please try again.",
        severity: "error",
      });
    }
  };

  return (
    <PageContent
      maxWidth="1280px"
      sx={{ pb: { xs: 5, md: 8 }, pt: { xs: 2, sm: 3 } }}
    >
      <MovieHero
        movie={movie}
        isBookmarked={isBookmarked}
        onToggleBookmark={handleToggleBookmark}
        isBookmarkLoading={toggleWatchlist.isPending}
        userRating={userRating ?? null}
        onOpenRating={handleOpenRating}
        isRatingLoading={rateMovie.isPending}
        onShare={handleShare}
        isShareDisabled={movie.id === undefined}
      />

      {authPrompt && (
        <Alert
          severity="info"
          onClose={() => setAuthPrompt(null)}
          action={
            <Button color="inherit" size="small" onClick={navigateToLogin}>
              Sign in
            </Button>
          }
          sx={{ maxWidth: 780, mt: 1 }}
        >
          {authPrompt}
        </Alert>
      )}

      <Synopsis text={movie.description} />

      <MovieRatingDialog
        currentRating={userRating ?? null}
        errorMessage={ratingError}
        isPending={rateMovie.isPending}
        movieTitle={movie.primaryTitle?.trim() || "this movie"}
        onClose={() => {
          setIsRatingOpen(false);
          setRatingError(null);
        }}
        onSubmit={handleRate}
        open={isRatingOpen}
      />

      <Snackbar
        open={feedback !== null}
        autoHideDuration={4000}
        onClose={() => setFeedback(null)}
        anchorOrigin={{ horizontal: "center", vertical: "bottom" }}
      >
        <Alert
          severity={feedback?.severity ?? "success"}
          variant="filled"
          onClose={() => setFeedback(null)}
        >
          {feedback?.message ?? ""}
        </Alert>
      </Snackbar>
    </PageContent>
  );
};

const MovieDetailSkeleton = () => (
  <PageContent
    maxWidth="1280px"
    sx={{ pb: { xs: 5, md: 8 }, pt: { xs: 2, sm: 3 } }}
  >
    <Box aria-label="Loading movie details" role="status">
      <Skeleton
        variant="rounded"
        sx={{ height: { xs: 220, sm: 300, md: 430 }, width: "100%" }}
      />
      <Box
        sx={{
          display: "grid",
          gap: 2,
          gridTemplateColumns: { xs: "104px 1fr", md: "220px 1fr 260px" },
          mt: { xs: -7, md: -20 },
          mx: { xs: 1.5, sm: 3, md: 4 },
          position: "relative",
        }}
      >
        <Skeleton variant="rounded" sx={{ aspectRatio: "2 / 3" }} />
        <Box sx={{ pt: { xs: 7, md: 20 } }}>
          <Skeleton variant="text" width="75%" height={48} />
          <Skeleton variant="text" width="42%" />
          <Skeleton variant="text" width="55%" />
        </Box>
        <Box sx={{ display: { xs: "none", md: "block" }, pt: 20 }}>
          <Skeleton variant="rounded" height={70} sx={{ mb: 1 }} />
          <Skeleton variant="rounded" height={70} />
        </Box>
      </Box>
    </Box>
  </PageContent>
);

export const parseMovieId = (movieId: string | null): number | null => {
  if (movieId === null || !/^\d+$/.test(movieId)) {
    return null;
  }
  const parsedMovieId = Number(movieId);
  return Number.isSafeInteger(parsedMovieId) && parsedMovieId > 0
    ? parsedMovieId
    : null;
};

export default MovieDetailPage;
