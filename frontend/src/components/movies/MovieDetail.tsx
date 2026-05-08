import { useLocation } from "react-router";
import React, { useEffect } from "react";
import { CardMedia, Container, Paper } from "@mui/material";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";
import { useDispatch, useSelector } from "react-redux";
import { Dispatch } from "../../redux/store";
import { State as MovieState } from "../../redux/model/movies";
import { getMinioImageUrl, MinioImageSize } from "../../utils/imageUrlParser";
import placeholderSearch from "../../assets/img/placeholder_search.png";

const MovieDetail = () => {
  const dispatch = useDispatch<Dispatch>();

  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const movieId = queryParams.get("id");

  const movie = useSelector(
    (state: { movies: MovieState }) => state.movies.movie,
  );

  const imageUrl = movie?.imageUrlToken
    ? getMinioImageUrl(movie?.imageUrlToken, MinioImageSize.Large)
    : undefined;

  useEffect(() => {
    if (movieId !== null) {
      dispatch.movies.loadMovieById(parseInt(movieId));
    }
  }, [dispatch.movies, movieId]);

  console.log(
    "movie detail rendered with imageUrlToken: " + movie?.imageUrlToken,
  );

  return (
    <>
      <div>
        {Object.keys(movie).length > 0 ? (
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
                {i18n.movieDetails.loadingError(movieId)}
              </Typography>
            </Paper>
          </Container>
        )}
      </div>
    </>
  );
};

export default MovieDetail;
