import type { Movie } from "./movie";
import { MovieType } from "./movie";

const SERIES_TYPES = new Set<string>([
  MovieType.TvSeries,
  MovieType.TvMiniSeries,
]);

export const humanizeMovieValue = (value: string): string =>
  value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

export const formatMovieYear = (movie: Movie): string | null => {
  if (!movie.startYear) {
    return null;
  }

  if (
    movie.movieType &&
    SERIES_TYPES.has(movie.movieType) &&
    movie.endYear
  ) {
    return `${movie.startYear}–${movie.endYear}`;
  }

  return String(movie.startYear);
};

export const formatMovieRuntime = (
  runtimeMinutes: number | null | undefined,
): string | null => {
  if (!runtimeMinutes || runtimeMinutes < 1) {
    return null;
  }

  const hours = Math.floor(runtimeMinutes / 60);
  const minutes = runtimeMinutes % 60;

  if (hours === 0) {
    return `${minutes}m`;
  }
  if (minutes === 0) {
    return `${hours}h`;
  }
  return `${hours}h ${minutes}m`;
};

export const getMovieMetaItems = (movie: Movie): string[] =>
  [
    formatMovieYear(movie),
    movie.movieType ? humanizeMovieValue(movie.movieType) : null,
    formatMovieRuntime(movie.runtimeMinutes),
    movie.adult ? "18+" : null,
  ].filter((item): item is string => Boolean(item));

export const getMovieGenreLabels = (movie: Movie): string[] =>
  movie.movieGenre
    ? Array.from(movie.movieGenre).map((genre) => humanizeMovieValue(genre))
    : [];

export const getOriginalTitle = (movie: Movie): string | null => {
  const primaryTitle = movie.primaryTitle?.trim();
  const originalTitle = movie.originalTitle?.trim();

  if (!originalTitle || originalTitle === primaryTitle) {
    return null;
  }

  return originalTitle;
};
