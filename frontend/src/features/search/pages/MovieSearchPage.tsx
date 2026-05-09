import { Grid } from "@mui/material";
import React from "react";
import { useLocation } from "react-router";
import { MovieCard } from "../../catalog";
import { useQuery } from "@tanstack/react-query";
import { searchQueries } from "../api/searchQueries";
import PageContent from "../../../shared/layout/PageContent";

const MovieSearchPage = () => {
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
    <PageContent maxWidth="900px">
      <Grid container spacing={1}>
        {movies.map((movie) => (
          <Grid key={movie.id} size={{ xs: 12 }}>
            <MovieCard {...movie} />
          </Grid>
        ))}
      </Grid>
    </PageContent>
  );
};

export default MovieSearchPage;
