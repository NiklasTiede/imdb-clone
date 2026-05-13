import { useLocation } from "react-router";
import { Snackbar, Typography } from "@mui/material";
import Divider from "@mui/material/Divider";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { i18n } from "../../../i18n";
import { authSession } from "../../../shared/auth/authSession";
import { useAuthSession } from "../../../shared/auth/useAuthSession";
import PageContent from "../../../shared/layout/PageContent";
import Surface from "../../../shared/layout/Surface";
import { movieQueries } from "../api/movieQueries";
import { MovieHero } from "../components/MovieHero";
import Synopsis from "../components/Synopsis";
import {
  rateMovieMutationOptions,
  ratingQueries,
  toggleWatchlistMutationOptions,
  watchlistQueries,
} from "../../engagement";

const MovieDetailPage = () => {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const movieId = parseMovieId(queryParams.get("id"));
  const isAuthenticated = useAuthSession();
  const username = isAuthenticated ? authSession.getUsername() : null;
  const queryClient = useQueryClient();

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data: movie } = useQuery(movieQueries.detail(movieId));

  const { data: watchedMovieIds } = useQuery(
    watchlistQueries.movieIds({ username }),
  );

  const { data: userRating } = useQuery(
    ratingQueries.userRatingForMovie({ movieId, username }),
  );

  const toggleWatchlist = useMutation({
    ...toggleWatchlistMutationOptions(queryClient),
    onError: () =>
      setErrorMessage("Could not update your watchlist. Please try again."),
  });

  const rateMovie = useMutation({
    ...rateMovieMutationOptions(queryClient),
    onError: () =>
      setErrorMessage("Could not save your rating. Please try again."),
  });

  if (!movie) {
    return (
      <PageContent maxWidth="760px">
        <Surface sx={{ p: { xs: 2, sm: 4 }, mt: 7, fontSize: 18 }}>
          <Typography sx={{ textAlign: "center" }}>
            {i18n.movieDetails.loadingError(queryParams.get("id"))}
          </Typography>
        </Surface>
      </PageContent>
    );
  }

  const isBookmarked = Boolean(
    movie.id !== undefined && watchedMovieIds?.has(movie.id),
  );

  const handleToggleBookmark = () => {
    if (!username || movie.id === undefined) {
      setErrorMessage("Please log in to manage your watchlist.");
      return;
    }
    toggleWatchlist.mutate({ movieId: movie.id, isBookmarked });
  };

  const handleRate = (score: number) => {
    if (!username || movie.id === undefined) {
      setErrorMessage("Please log in to rate this movie.");
      return;
    }
    rateMovie.mutate({ movieId: movie.id, score });
  };

  return (
    <PageContent maxWidth="760px">
      <Surface
        elevation={3}
        sx={{
          mt: { xs: 2, sm: 5 },
          overflow: "hidden",
        }}
      >
        <MovieHero
          movie={movie}
          isBookmarked={isBookmarked}
          onToggleBookmark={handleToggleBookmark}
          isBookmarkLoading={toggleWatchlist.isPending}
          userRating={userRating ?? null}
          onRate={handleRate}
        />
        <Divider />
        <Synopsis text={movie.description} />
      </Surface>
      <Snackbar
        open={errorMessage !== null}
        autoHideDuration={4000}
        onClose={() => setErrorMessage(null)}
        message={errorMessage ?? ""}
      />
    </PageContent>
  );
};

const parseMovieId = (movieId: string | null): number | null => {
  if (movieId === null) {
    return null;
  }
  const parsedMovieId = Number.parseInt(movieId, 10);
  return Number.isNaN(parsedMovieId) ? null : parsedMovieId;
};

export default MovieDetailPage;
