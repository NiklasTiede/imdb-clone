import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Box from "@mui/material/Box";
import Container from "@mui/material/Container";
import Snackbar from "@mui/material/Snackbar";
import { MovieSearchRequestMovieGenreEnum } from "../../../client/movies/generator-output";
import FeaturedMovieHero from "../components/FeaturedMovieHero";
import MovieCarousel from "../components/MovieCarousel";
import { authSession, useAuthSession } from "../../../shared/auth";
import { useFeaturedMovie } from "../api/useFeaturedMovie";
import { useMoviesByGenre } from "../api/useMoviesByGenre";
import {
  toggleWatchlistMutationOptions,
  watchlistQueries,
} from "../../engagement";
import { useNavigate } from "react-router";
import { useState } from "react";

export const HOME_CAROUSEL_LOOKBACK_YEARS = 30;

export const getHomeMinStartYear = (date = new Date()) =>
  date.getFullYear() - HOME_CAROUSEL_LOOKBACK_YEARS;

export const homeGenreRows = [
  {
    genre: MovieSearchRequestMovieGenreEnum.Drama,
    subtitle: "Highest-rated drama from the last 30 years",
    title: "Top drama",
    viewAllGenre: "DRAMA",
  },
  {
    genre: MovieSearchRequestMovieGenreEnum.Thriller,
    subtitle: "Edge-of-your-seat picks",
    title: "Top thrillers",
    viewAllGenre: "THRILLER",
  },
  {
    genre: MovieSearchRequestMovieGenreEnum.SciFi,
    subtitle: "Worlds beyond our own",
    title: "Top sci-fi",
    viewAllGenre: "SCI_FI",
  },
] as const;

const HomePage = () => {
  const navigate = useNavigate();
  const isAuthenticated = useAuthSession();
  const username = isAuthenticated ? authSession.getUsername() : null;
  const queryClient = useQueryClient();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const minStartYear = getHomeMinStartYear();

  const { data: featuredMovie = null, isLoading: isFeaturedLoading } =
    useFeaturedMovie();
  const dramaMovies = useMoviesByGenre({
    genre: homeGenreRows[0].genre,
    minStartYear,
  });
  const thrillerMovies = useMoviesByGenre({
    genre: homeGenreRows[1].genre,
    minStartYear,
  });
  const sciFiMovies = useMoviesByGenre({
    genre: homeGenreRows[2].genre,
    minStartYear,
  });
  const { data: watchedMovieIds } = useQuery(
    watchlistQueries.movieIds({ username }),
  );

  const toggleWatchlist = useMutation({
    ...toggleWatchlistMutationOptions(queryClient),
    onError: () =>
      setErrorMessage("Could not update your watchlist. Please try again."),
  });

  const isFeaturedBookmarked = Boolean(
    featuredMovie?.id !== undefined && watchedMovieIds?.has(featuredMovie.id),
  );

  const handleViewMovie = () => {
    if (featuredMovie?.id !== undefined) {
      navigate(`/movie?id=${featuredMovie.id}`);
    }
  };

  const handleToggleFeaturedBookmark = () => {
    if (!username || featuredMovie?.id === undefined) {
      setErrorMessage("Please log in to manage your watchlist.");
      return;
    }

    toggleWatchlist.mutate({
      isBookmarked: isFeaturedBookmarked,
      movieId: featuredMovie.id,
    });
  };

  return (
    <Box sx={{ backgroundColor: "background.default", minHeight: "100vh" }}>
      <Container
        maxWidth="xl"
        sx={{ px: { xs: 0, md: 4 }, py: { xs: 3, md: 4 } }}
      >
        <Box sx={{ px: { xs: 2, md: 0 } }}>
          <FeaturedMovieHero
            movie={featuredMovie}
            loading={isFeaturedLoading}
            isBookmarked={isFeaturedBookmarked}
            isBookmarkLoading={toggleWatchlist.isPending}
            onToggleBookmark={handleToggleFeaturedBookmark}
            onViewMovie={handleViewMovie}
          />
        </Box>

        <MovieCarousel
          title={homeGenreRows[0].title}
          subtitle={homeGenreRows[0].subtitle}
          movies={dramaMovies.data ?? []}
          onViewAll={() =>
            navigate(
              `/movie-search?genre=${homeGenreRows[0].viewAllGenre}&minYear=${minStartYear}&sort=rating_desc`,
            )
          }
          loading={dramaMovies.isLoading}
        />

        <MovieCarousel
          title={homeGenreRows[1].title}
          subtitle={homeGenreRows[1].subtitle}
          movies={thrillerMovies.data ?? []}
          onViewAll={() =>
            navigate(
              `/movie-search?genre=${homeGenreRows[1].viewAllGenre}&minYear=${minStartYear}&sort=rating_desc`,
            )
          }
          loading={thrillerMovies.isLoading}
        />

        <MovieCarousel
          title={homeGenreRows[2].title}
          subtitle={homeGenreRows[2].subtitle}
          movies={sciFiMovies.data ?? []}
          onViewAll={() =>
            navigate(
              `/movie-search?genre=${homeGenreRows[2].viewAllGenre}&minYear=${minStartYear}&sort=rating_desc`,
            )
          }
          loading={sciFiMovies.isLoading}
        />
      </Container>

      <Snackbar
        open={errorMessage !== null}
        autoHideDuration={4000}
        onClose={() => setErrorMessage(null)}
        message={errorMessage ?? ""}
      />
    </Box>
  );
};

export default HomePage;
