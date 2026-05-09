import { Container, Grid } from "@mui/material";
import React from "react";
import { useLocation } from "react-router";
import MovieCard from "./MovieCard";
import { useQuery } from "@tanstack/react-query";
import { searchQueries } from "../../features/search";

const MovieSearch = () => {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const queryTerm = queryParams.get("query");

  const { data } = useQuery(
    searchQueries.movies({
      filters: {},
      page: 0,
      query: queryTerm,
      size: 20,
    }),
  );
  const movies = data?.content ?? [];

  return (
    <Container maxWidth={"md"} sx={{ padding: 3, marginTop: 0 }}>
      <Grid container spacing={1}>
        {movies.map((movie) => (
          <Grid key={movie.id} size={{ xs: 12 }}>
            <MovieCard {...movie} />
          </Grid>
        ))}
      </Grid>
    </Container>
  );
};

export default MovieSearch;
