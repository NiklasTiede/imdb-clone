import { PagedResponseWatchedMovieRecord } from "../../../../client/movies/generator-output";
import { accountEngagementApi } from "../../../../shared/api/moviesApi";

type CurrentUserWatchlistParams = {
  page: number;
  size: number;
  username: string | null;
};

type WatchlistMovieIdsParams = {
  username: string | null;
};

const MOVIE_IDS_PAGE_SIZE = 30;

export const watchlistQueryKeys = {
  all: ["watchlist"] as const,
  currentUser: (username: string | null) =>
    [...watchlistQueryKeys.all, "current-user", username] as const,
  currentUserItems: ({
    page,
    size,
    username,
  }: {
    page: number;
    size: number;
    username: string | null;
  }) => [...watchlistQueryKeys.currentUser(username), page, size] as const,
  currentUserMovieIds: (username: string | null) =>
    [...watchlistQueryKeys.currentUser(username), "movie-ids"] as const,
};

const getWatchlistMovieIds = (
  watchlist: PagedResponseWatchedMovieRecord,
): number[] =>
  (watchlist.content ?? [])
    .map((watchedMovie) => watchedMovie.movieId)
    .filter((movieId): movieId is number => movieId !== undefined);

const getCurrentUserWatchlistItems = async ({
  page,
  size,
  username,
}: CurrentUserWatchlistParams): Promise<PagedResponseWatchedMovieRecord> => {
  const normalizedUsername = username?.trim();

  if (!normalizedUsername) {
    throw new Error("Username is required to load the watchlist.");
  }

  const watchlistResponse =
    await accountEngagementApi.getWatchedMoviesByAccount(
      normalizedUsername,
      page,
      size,
    );
  return watchlistResponse.data;
};

const fetchCurrentUserWatchedMovieIds = async (
  username: string,
): Promise<Set<number>> => {
  const ids = new Set<number>();
  let page = 0;
  let isLast = false;
  while (!isLast) {
    const response = await accountEngagementApi.getWatchedMoviesByAccount(
      username,
      page,
      MOVIE_IDS_PAGE_SIZE,
    );
    getWatchlistMovieIds(response.data).forEach((id) => ids.add(id));
    isLast = response.data.last ?? true;
    page += 1;
  }
  return ids;
};

export const watchlistQueries = {
  currentUserItems: ({ page, size, username }: CurrentUserWatchlistParams) => {
    const normalizedUsername = username?.trim() || null;

    return {
      enabled: normalizedUsername !== null,
      queryFn: () =>
        getCurrentUserWatchlistItems({
          page,
          size,
          username: normalizedUsername,
        }),
      queryKey: watchlistQueryKeys.currentUserItems({
        page,
        size,
        username: normalizedUsername,
      }),
    };
  },

  movieIds: ({ username }: WatchlistMovieIdsParams) => {
    const normalizedUsername = username?.trim() || null;

    return {
      enabled: normalizedUsername !== null,
      queryFn: () => {
        if (normalizedUsername === null) {
          throw new Error("Username is required to load watched movie ids.");
        }
        return fetchCurrentUserWatchedMovieIds(normalizedUsername);
      },
      queryKey: watchlistQueryKeys.currentUserMovieIds(normalizedUsername),
    };
  },
};
