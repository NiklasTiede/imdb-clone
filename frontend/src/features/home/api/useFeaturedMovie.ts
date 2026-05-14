import { useQuery } from "@tanstack/react-query";
import {
  MovieSearchRequestMovieTypeEnum,
  type MovieRecord,
} from "../../../client/movies/generator-output";
import { moviesApi, searchApi } from "../../../shared/api/moviesApi";
import { pickDailyIndex } from "../utils/pickDailyIndex";

const FEATURED_MOVIE_POOL_SIZE = 30;
const FEATURED_MOVIE_LOOKBACK_YEARS = 30;
const MIN_FEATURED_IMDB_RATING = 7.5;

export const getFeaturedMinStartYear = (date = new Date()) =>
  date.getFullYear() - FEATURED_MOVIE_LOOKBACK_YEARS;

export const featuredMovieQuery = (date = new Date()) => {
  const dayKey = date.toISOString().slice(0, 10);
  const minStartYear = getFeaturedMinStartYear(date);

  return {
    queryFn: async (): Promise<MovieRecord | null> => {
      const response = await searchApi.search(
        "",
        {
          minStartYear,
          movieType: MovieSearchRequestMovieTypeEnum.Movie,
        },
        0,
        FEATURED_MOVIE_POOL_SIZE,
      );
      const eligibleMovies = (response.data.content ?? [])
        .filter(
          (movie) =>
            Boolean(movie.imageUrlToken) &&
            (movie.imdbRating ?? 0) >= MIN_FEATURED_IMDB_RATING,
        )
        .sort((left, right) => (right.imdbRating ?? 0) - (left.imdbRating ?? 0));
      const selectedIndex = pickDailyIndex(eligibleMovies.length, date);
      const selectedMovie =
        selectedIndex === null ? null : eligibleMovies[selectedIndex];

      if (selectedMovie?.id === undefined) {
        return selectedMovie;
      }

      const detailsResponse = await moviesApi.getMovieById(selectedMovie.id);
      return detailsResponse.data;
    },
    queryKey: ["home", "featured-movie", dayKey] as const,
  };
};

export const useFeaturedMovie = () => useQuery(featuredMovieQuery());
