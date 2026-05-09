import { useLocation } from "react-router";
import React from "react";
import { Paper } from "@mui/material";
import Typography from "@mui/material/Typography";
import { i18n } from "../../../i18n";
import { useQuery } from "@tanstack/react-query";
import { movieQueries } from "../api/movieQueries";
import { MinioImageSize, PosterImage } from "../../../shared/media";
import PageContent from "../../../shared/layout/PageContent";

const MovieDetailPage = () => {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const movieId = parseMovieId(queryParams.get("id"));
  const { data: movie } = useQuery(movieQueries.detail(movieId));

  return (
    <>
      <div>
        {movie ? (
          <PageContent maxWidth="420px">
            <Paper
              elevation={3}
              sx={{
                p: { xs: 2, sm: 4 },
                mt: { xs: 2, sm: 7 },
                fontSize: 18,
              }}
            >
              <Typography variant={"inherit"} sx={{ textAlign: "center" }}>
                {movie.primaryTitle}, {movie.startYear}
              </Typography>
              <PosterImage
                imageUrlToken={movie.imageUrlToken}
                size={MinioImageSize.Large}
                sx={{
                  width: "100%",
                  maxWidth: 300,
                  aspectRatio: "2 / 3",
                  height: "auto",
                  mx: "auto",
                  p: 1,
                }}
              />
              <Typography variant={"inherit"} sx={{ textAlign: "center" }}>
                {movie.description}
              </Typography>
            </Paper>
          </PageContent>
        ) : (
          <PageContent maxWidth="420px">
            <Paper
              elevation={3}
              sx={{ p: { xs: 2, sm: 4 }, mt: 7, fontSize: 18 }}
            >
              <Typography variant={"inherit"} sx={{ textAlign: "center" }}>
                {i18n.movieDetails.loadingError(queryParams.get("id"))}
              </Typography>
            </Paper>
          </PageContent>
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
