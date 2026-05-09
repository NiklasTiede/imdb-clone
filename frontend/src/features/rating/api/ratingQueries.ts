import { accountApi } from "../../../shared/api/moviesApi";

type UserRatingForMovieParams = {
  movieId: number | null;
  username: string | null;
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

export const ratingQueries = {
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
