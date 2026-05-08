import { Container, Grid } from "@mui/material";
import { shallowEqual, useDispatch, useSelector } from "react-redux";
import { Dispatch } from "../../redux/store";
import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { MovieRecord } from "../../client/movies/generator-output";
import MovieCard from "./MovieCard";
import { State as SearchState } from "../../redux/model/search";

const MovieSearch = () => {
  const dispatch = useDispatch<Dispatch>();

  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const queryTerm = queryParams.get("query");

  // const movies: Array<MovieRecord> = useSelector(
  //     (state: { search: SearchState }) => state.search.movies,
  //     shallowEqual
  // );

  const movies: Array<MovieRecord> = useSelector(
    (state: { search?: SearchState }) => state.search?.movies ?? [],
    shallowEqual,
  );

  useEffect(() => {
    const payload = {
      query: queryTerm,
      requestSearchParams: {},
      page: 0,
      size: 20,
    };
    dispatch.search.searchMovies(payload);
  }, [dispatch.search, queryTerm]);

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
