import { Container, Grid, useTheme } from "@mui/material";
import { tokens } from "../../theme";
import { useNavigate } from "react-router-dom";
import {shallowEqual, useDispatch, useSelector} from "react-redux";
import { Dispatch } from "../../redux/store";
import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import {
  Movie,
  MovieSearchRequestMovieTypeEnum,
} from "../../client/movies/generator-output";
import MovieCard from "./MovieCard";
import { State as SearchState } from "../../redux/model/search";

const MovieSearch = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const queryTerm = queryParams.get("query");

  // const movies: Array<Movie> = useSelector(
  //     (state: { search: SearchState }) => state.search.movies,
  //     shallowEqual
  // );

  const movies: Array<Movie> = useSelector(
      (state: { search?: SearchState }) => state.search?.movies ?? [],
      shallowEqual
  );

  let payload: any = {
    query: queryTerm,
    requestSearchParams: {
      // minRuntimeMinutes: 80,
      // maxRuntimeMinutes: 230,
      // minStartYear: 2010,
      // maxStartYear: 2022,
      // movieGenre: ["HORROR"],
      // movieType: MovieSearchRequestMovieTypeEnum.Movie,
      // adult: false,
    },
    page: 0,
    size: 20,
  };

  useEffect(() => {
    dispatch.search.searchMovies(payload);
  }, [queryTerm]);

  console.log(movies);

  return (
    <Container maxWidth={"md"} sx={{ padding: 3, marginTop: 0 }}>
      <Grid container spacing={1}>
        {movies.map((movie) => (
          <Grid item key={movie.id} xs={12}>
            <MovieCard {...movie} />
          </Grid>
        ))}
      </Grid>
    </Container>
  );
};

export default MovieSearch;
