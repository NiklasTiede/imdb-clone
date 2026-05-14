import {
  MovieRecordMovieGenreEnum,
  MovieRecordMovieTypeEnum,
  MovieSearchRequestMovieGenreEnum,
  MovieSearchRequestMovieTypeEnum,
} from "../../../client/movies/generator-output";
import type { MovieRecord } from "../../../client/movies/generator-output";

export type Movie = MovieRecord;

export const MovieGenre = MovieRecordMovieGenreEnum;
export type MovieGenre = MovieRecordMovieGenreEnum;

export const MovieType = MovieRecordMovieTypeEnum;
export type MovieType = MovieRecordMovieTypeEnum;

export const MovieSearchGenre = MovieSearchRequestMovieGenreEnum;
export type MovieSearchGenre = MovieSearchRequestMovieGenreEnum;

export const MovieSearchType = MovieSearchRequestMovieTypeEnum;
export type MovieSearchType = MovieSearchRequestMovieTypeEnum;
