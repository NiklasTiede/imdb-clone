import { useQuery } from "@tanstack/react-query";
import type { MovieRecord } from "../client/movies/generator-output";
import { moviesApi } from "../shared/api/moviesApi";
import { pickDailyIndex } from "../utils/pickDailyIndex";

const FEATURED_MOVIE_POOL_SIZE = 30;
const MIN_FEATURED_IMDB_RATING = 7.5;

export const featuredMovieQuery = (date = new Date()) => {
  const dayKey = date.toISOString().slice(0, 10);

  return {
    queryFn: async (): Promise<MovieRecord | null> => {
      const response = await moviesApi.getMoviesByIds(
        {},
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

      return selectedIndex === null ? null : eligibleMovies[selectedIndex];
    },
    queryKey: ["home", "featured-movie", dayKey] as const,
  };
};

export const useFeaturedMovie = () => useQuery(featuredMovieQuery());
