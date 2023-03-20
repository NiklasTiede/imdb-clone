import { useLocation, useNavigate } from "react-router-dom";
import React, { useEffect } from "react";
import {CardMedia, Container, Paper, useTheme} from "@mui/material";
import Typography from "@mui/material/Typography";
import { i18n } from "../../i18n";
import { tokens } from "../../theme";
import { useDispatch, useSelector } from "react-redux";
import { Dispatch } from "../../redux/store";
import { State as MovieState } from "../../redux/model/movies";

const MovieDetail = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const movieId = queryParams.get("id");

  const movie = useSelector(
    (state: { movies: MovieState }) => state.movies.movie
  );

  const imageUrl = `http://192.168.178.49:9000/imdb-clone/movies/${movie?.imageUrlToken}_size_600x900.jpg`;


  useEffect(() => {
    if (movieId !== null) {
      dispatch.movies.loadMovieById(parseInt(movieId));
    }
  }, [movieId]);

  console.log("movie detail rendered with imageUrlToken: " + movie?.imageUrlToken);

  return (
    <>
      <div>
        {Object.keys(movie).length > 0 ? (
          <Container maxWidth={"xs"}>
            <Paper
              elevation={3}
              sx={{ padding: 4, marginTop: 10, fontSize: 18 }}
            >
              <Typography variant={"inherit"} textAlign={"center"}>
                {movie.primaryTitle}, {movie.startYear}
              </Typography>
              <CardMedia
                  component="img"
                  alt="movie poster"
                  sx={{ width: 300, height: 450, padding: 1 }}
                  src={movie.imageUrlToken ? imageUrl : require("../../assets/img/placeholder_search.png")}
              />
              <Typography variant={"inherit"} textAlign={"center"}>
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
              <Typography variant={"inherit"} textAlign={"center"}>
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
