import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import Stack from "@mui/material/Stack";
import { useQuery } from "@tanstack/react-query";
import React from "react";
import { MovieCard } from "../../../catalog";
import { i18n } from "../../../../i18n";
import PageContent from "../../../../shared/layout/PageContent";
import SectionHeading from "../../../../shared/layout/SectionHeading";
import { getUsername } from "../../../../utils/jwtHelper";
import { RatedMovie, ratingQueries } from "../api/ratingQueries";

const YourRatingsPage = () => {
  const username = getUsername();
  const { data, isError, isLoading } = useQuery(
    ratingQueries.currentUserMovies({
      page: 0,
      size: 20,
      username,
    }),
  );
  const ratedMovies = data?.content ?? [];

  return (
    <PageContent maxWidth="900px">
      <Stack spacing={2}>
        <SectionHeading>{i18n.ratings.heading}</SectionHeading>

        {isLoading && <CircularProgress aria-label="Loading ratings" />}

        {isError && <Alert severity="error">{i18n.ratings.loadingError}</Alert>}

        {!isLoading && !isError && ratedMovies.length === 0 && (
          <Alert severity="info">{i18n.ratings.empty}</Alert>
        )}

        {ratedMovies.length > 0 && (
          <Grid container spacing={1}>
            {ratedMovies.map((ratedMovie) => (
              <Grid key={ratedMovie.movie.id} size={{ xs: 12 }}>
                <RatedMovieCard ratedMovie={ratedMovie} />
              </Grid>
            ))}
          </Grid>
        )}
      </Stack>
    </PageContent>
  );
};

type RatedMovieCardProps = {
  ratedMovie: RatedMovie;
};

const RatedMovieCard = ({ ratedMovie }: RatedMovieCardProps) => (
  <Box sx={{ position: "relative" }}>
    <Box
      sx={{
        position: { xs: "static", sm: "absolute" },
        display: "flex",
        justifyContent: { xs: "flex-start", sm: "flex-end" },
        top: 8,
        right: 8,
        zIndex: 1,
        mb: { xs: 0.5, sm: 0 },
      }}
    >
      <Chip
        label={`Your rating: ${ratedMovie.rating}/10`}
        color="primary"
        size="small"
        sx={{ fontWeight: 600 }}
      />
    </Box>
    <MovieCard {...ratedMovie.movie} />
  </Box>
);

export default YourRatingsPage;
