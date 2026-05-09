import {
  MovieRecord,
  PagedResponseRatingRecord,
} from "../../../client/movies/generator-output";
import { accountApi } from "../../../shared/api/moviesApi";
import { moviesApi } from "../../../shared/api/moviesApi";

type UserRatingForMovieParams = {
  movieId: number | null;
  username: string | null;
};

type CurrentUserRatedMoviesParams = {
  page: number;
  size: number;
  username: string | null;
};

export type RatedMovie = {
  movie: MovieRecord;
  rating: number;
};

export type RatedMoviesResponse = Omit<PagedResponseRatingRecord, "content"> & {
  content: RatedMovie[];
};

const RATINGS_PAGE_SIZE = 30;

const fetchUserRatingForMovie = async ({
  movieId,
  username,
}: {
  movieId: number;
  username: string;
}): Promise<number | null> => {
  let page = 0;
  let isLast = false;
  while (!isLast) {
    const response = await accountApi.getRatingsByAccount(
      username,
      page,
      RATINGS_PAGE_SIZE,
    );
    const found = (response.data.content ?? []).find(
      (rating) => rating.movieId === movieId,
    );
    if (found?.rating !== undefined) {
      return found.rating;
    }
    isLast = response.data.last ?? true;
    page += 1;
  }
  return null;
};

const getCurrentUserRatedMovies = async ({
  page,
  size,
  username,
}: CurrentUserRatedMoviesParams): Promise<RatedMoviesResponse> => {
  const normalizedUsername = username?.trim();

  if (!normalizedUsername) {
    throw new Error("Username is required to load rated movies.");
  }

  const ratingsResponse = await accountApi.getRatingsByAccount(
    normalizedUsername,
    page,
    size,
  );
  const ratings = ratingsResponse.data;
  const ratedMovieIds = (ratings.content ?? [])
    .map((rating) => rating.movieId)
    .filter((movieId): movieId is number => movieId !== undefined);

  if (ratedMovieIds.length === 0) {
    return {
      content: [],
      last: ratings.last,
      page: ratings.page,
      size: ratings.size,
      totalElements: ratings.totalElements,
      totalPages: ratings.totalPages,
    };
  }

  const moviesResponse = await moviesApi.getMoviesByIds(
    { movieIds: ratedMovieIds },
    0,
    ratedMovieIds.length,
  );
  const moviesById = new Map(
    (moviesResponse.data.content ?? [])
      .filter((movie) => movie.id !== undefined)
      .map((movie) => [movie.id, movie]),
  );

  return {
    content: (ratings.content ?? [])
      .map((rating) => {
        if (rating.movieId === undefined || rating.rating === undefined) {
          return null;
        }
        const movie = moviesById.get(rating.movieId);
        return movie ? { movie, rating: rating.rating } : null;
      })
      .filter((ratedMovie): ratedMovie is RatedMovie => ratedMovie !== null),
    last: ratings.last,
    page: ratings.page,
    size: ratings.size,
    totalElements: ratings.totalElements,
    totalPages: ratings.totalPages,
  };
};

export const ratingQueries = {
  currentUserMovies: ({
    page,
    size,
    username,
  }: CurrentUserRatedMoviesParams) => {
    const normalizedUsername = username?.trim() || null;

    return {
      enabled: normalizedUsername !== null,
      queryFn: () =>
        getCurrentUserRatedMovies({
          page,
          size,
          username: normalizedUsername,
        }),
      queryKey: [
        "rating",
        "current-user",
        normalizedUsername,
        "movies",
        page,
        size,
      ] as const,
    };
  },

  userRatingForMovie: ({ movieId, username }: UserRatingForMovieParams) => {
    const normalizedUsername = username?.trim() || null;
    const enabled = movieId !== null && normalizedUsername !== null;

    return {
      enabled,
      queryFn: () => {
        if (movieId === null || normalizedUsername === null) {
          throw new Error(
            "Movie id and username are required to load the user rating.",
          );
        }
        return fetchUserRatingForMovie({
          movieId,
          username: normalizedUsername,
        });
      },
      queryKey: [
        "rating",
        "current-user",
        normalizedUsername,
        "movie",
        movieId,
      ] as const,
    };
  },
};
