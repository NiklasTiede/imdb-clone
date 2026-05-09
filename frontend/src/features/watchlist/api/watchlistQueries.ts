import {
  PagedResponseMovieRecord,
  PagedResponseWatchedMovieRecord,
} from "../../../client/movies/generator-output";
import { accountApi, moviesApi } from "../../../shared/api/moviesApi";

type CurrentUserWatchlistParams = {
  page: number;
  size: number;
  username: string | null;
};

const getWatchlistMovieIds = (
  watchlist: PagedResponseWatchedMovieRecord,
): number[] =>
  (watchlist.content ?? [])
    .map((watchedMovie) => watchedMovie.movieId)
    .filter((movieId): movieId is number => movieId !== undefined);

const getCurrentUserWatchlistMovies = async ({
  page,
  size,
  username,
}: CurrentUserWatchlistParams): Promise<PagedResponseMovieRecord> => {
  const normalizedUsername = username?.trim();

  if (!normalizedUsername) {
    throw new Error("Username is required to load the watchlist.");
  }

  const watchlistResponse = await accountApi.getWatchedMoviesByAccount(
    normalizedUsername,
    page,
    size,
  );
  const watchlist = watchlistResponse.data;
  const movieIds = getWatchlistMovieIds(watchlist);

  if (movieIds.length === 0) {
    return {
      content: [],
      last: watchlist.last,
      page: watchlist.page,
      size: watchlist.size,
      totalElements: watchlist.totalElements,
      totalPages: watchlist.totalPages,
    };
  }

  const moviesResponse = await moviesApi.getMoviesByIds(
    { movieIds },
    0,
    movieIds.length,
  );

  return {
    ...moviesResponse.data,
    last: watchlist.last,
    page: watchlist.page,
    size: watchlist.size,
    totalElements: watchlist.totalElements,
    totalPages: watchlist.totalPages,
  };
};

export const watchlistQueries = {
  currentUserMovies: ({ page, size, username }: CurrentUserWatchlistParams) => {
    const normalizedUsername = username?.trim() || null;

    return {
      enabled: normalizedUsername !== null,
      queryFn: () =>
        getCurrentUserWatchlistMovies({
          page,
          size,
          username: normalizedUsername,
        }),
      queryKey: [
        "watchlist",
        "current-user",
        normalizedUsername,
        page,
        size,
      ] as const,
    };
  },
};
