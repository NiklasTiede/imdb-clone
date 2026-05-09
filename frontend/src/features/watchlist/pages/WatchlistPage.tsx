import Alert from "@mui/material/Alert";
import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useQuery } from "@tanstack/react-query";
import React from "react";
import { MovieCard } from "../../catalog";
import { i18n } from "../../../i18n";
import PageContent from "../../../shared/layout/PageContent";
import { getUsername } from "../../../utils/jwtHelper";
import { watchlistQueries } from "../api/watchlistQueries";

const WatchlistPage = () => {
  const username = getUsername();
  const { data, isError, isLoading } = useQuery(
    watchlistQueries.currentUserMovies({
      page: 0,
      size: 20,
      username,
    }),
  );
  const movies = data?.content ?? [];

  return (
    <PageContent maxWidth="900px">
      <Stack spacing={2}>
        <Typography variant="h5" component="h1">
          {i18n.watchlist.heading}
        </Typography>

        {isLoading && <CircularProgress aria-label="Loading watchlist" />}

        {isError && (
          <Alert severity="error">{i18n.watchlist.loadingError}</Alert>
        )}

        {!isLoading && !isError && movies.length === 0 && (
          <Alert severity="info">{i18n.watchlist.empty}</Alert>
        )}

        {movies.length > 0 && (
          <Grid container spacing={1}>
            {movies.map((movie) => (
              <Grid key={movie.id} size={{ xs: 12 }}>
                <MovieCard {...movie} />
              </Grid>
            ))}
          </Grid>
        )}
      </Stack>
    </PageContent>
  );
};

export default WatchlistPage;
