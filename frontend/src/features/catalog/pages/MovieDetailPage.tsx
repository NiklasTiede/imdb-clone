import { useLocation } from "react-router";
import React from "react";
import { Container, Paper } from "@mui/material";
import Typography from "@mui/material/Typography";
import { i18n } from "../../../i18n";
import { useQuery } from "@tanstack/react-query";
import { movieQueries } from "../api/movieQueries";
import { MinioImageSize, PosterImage } from "../../../shared/media";

const MovieDetailPage = () => {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const movieId = parseMovieId(queryParams.get("id"));
  const { data: movie } = useQuery(movieQueries.detail(movieId));

  return (
    <>
      <div>
        {movie ? (
          <Container maxWidth={"xs"}>
            <Paper
              elevation={3}
              sx={{ padding: 4, marginTop: 10, fontSize: 18 }}
            >
              <Typography variant={"inherit"} sx={{ textAlign: "center" }}>
                {movie.primaryTitle}, {movie.startYear}
              </Typography>
              <PosterImage
                imageUrlToken={movie.imageUrlToken}
                size={MinioImageSize.Large}
                sx={{ width: 300, height: 450, padding: 1 }}
              />
              <Typography variant={"inherit"} sx={{ textAlign: "center" }}>
                {movie.description}
              </Typography>
            </Paper>
          </Container>
        ) : (
          <Container maxWidth={"xs"}>
            <Paper
              elevation={3}
              sx={{ padding: 4, marginTop: 10, fontSize: 18 }}
            >
              <Typography variant={"inherit"} sx={{ textAlign: "center" }}>
                {i18n.movieDetails.loadingError(queryParams.get("id"))}
              </Typography>
            </Paper>
          </Container>
        )}
      </div>
    </>
  );
};

const parseMovieId = (movieId: string | null) => {
  if (movieId === null) {
    return null;
  }
  const parsedMovieId = Number.parseInt(movieId, 10);
  return Number.isNaN(parsedMovieId) ? null : parsedMovieId;
};

export default MovieDetailPage;
