import { useLocation } from "react-router";
import React from "react";
import { CardMedia, Container, Paper } from "@mui/material";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";
import { getMinioImageUrl, MinioImageSize } from "../../utils/imageUrlParser";
import placeholderSearch from "../../assets/img/placeholder_search.png";
import { useQuery } from "@tanstack/react-query";
import { movieQueries } from "../../features/catalog";

const MovieDetail = () => {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const movieId = parseMovieId(queryParams.get("id"));
  const { data: movie } = useQuery(movieQueries.detail(movieId));

  const imageUrl = movie?.imageUrlToken
    ? getMinioImageUrl(movie?.imageUrlToken, MinioImageSize.Large)
    : undefined;

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
              <CardMedia
                component="img"
                alt="movie poster"
                sx={{ width: 300, height: 450, padding: 1 }}
                src={movie.imageUrlToken ? imageUrl : placeholderSearch}
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

export default MovieDetail;
