import { useQuery } from "@tanstack/react-query";
import {
  MovieSearchRequestMovieGenreEnum,
  type MovieRecord,
} from "../client/movies/generator-output";
import { searchApi } from "../shared/api/moviesApi";

const GENRE_PAGE_SIZE = 20;
const GENRE_MOVIE_LIMIT = 15;

type GenreMoviesQueryParams = {
  genre: MovieSearchRequestMovieGenreEnum;
  minStartYear: number;
};

export const genreMoviesQuery = ({
  genre,
  minStartYear,
}: GenreMoviesQueryParams) => ({
  queryFn: async (): Promise<MovieRecord[]> => {
    const response = await searchApi.search(
      "",
      {
        minStartYear,
        movieGenre: new Set([genre]),
      },
      0,
      GENRE_PAGE_SIZE,
    );

    return [...(response.data.content ?? [])]
      .sort((left, right) => (right.imdbRating ?? 0) - (left.imdbRating ?? 0))
      .slice(0, GENRE_MOVIE_LIMIT);
  },
  queryKey: ["home", "genre-movies", genre, minStartYear] as const,
});

export const useMoviesByGenre = (params: GenreMoviesQueryParams) =>
  useQuery(genreMoviesQuery(params));
