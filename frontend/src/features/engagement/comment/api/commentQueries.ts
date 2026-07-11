import { accountApi, commentApi } from "../../../../shared/api/moviesApi";
import type {
  CommentAuthor,
  MovieCommentsPage,
} from "../model/comment";

export const COMMENTS_PAGE_SIZE = 10;
const AUTHOR_BATCH_SIZE = 30;

export const commentQueryKeys = {
  all: ["comments"] as const,
  movie: (movieId: number) =>
    [...commentQueryKeys.all, "movie", movieId] as const,
  authors: (accountIds: number[]) =>
    ["account", "public-summaries", accountIds] as const,
};

const fetchMovieComments = async (
  movieId: number,
  page: number,
): Promise<MovieCommentsPage> => {
  const response = await commentApi.getCommentsByMovieId(
    movieId,
    page,
    COMMENTS_PAGE_SIZE,
  );
  return response.data;
};

const fetchPublicAccountSummaries = async (
  accountIds: number[],
): Promise<CommentAuthor[]> => {
  const summaries: CommentAuthor[] = [];
  for (let index = 0; index < accountIds.length; index += AUTHOR_BATCH_SIZE) {
    const response = await accountApi.getPublicAccountSummaries(
      accountIds.slice(index, index + AUTHOR_BATCH_SIZE),
    );
    summaries.push(...response.data);
  }
  return summaries;
};

export const commentQueries = {
  movie: (movieId: number) => ({
    enabled: Number.isSafeInteger(movieId) && movieId > 0,
    getNextPageParam: (lastPage: MovieCommentsPage) =>
      lastPage.last ? undefined : (lastPage.page ?? 0) + 1,
    initialPageParam: 0,
    queryFn: ({ pageParam }: { pageParam: number }) =>
      fetchMovieComments(movieId, pageParam),
    queryKey: commentQueryKeys.movie(movieId),
  }),

  authors: (accountIds: number[]) => {
    const normalizedAccountIds = [...new Set(accountIds)]
      .filter((accountId) => Number.isSafeInteger(accountId) && accountId > 0)
      .sort((left, right) => left - right);

    return {
      enabled: normalizedAccountIds.length > 0,
      queryFn: () => fetchPublicAccountSummaries(normalizedAccountIds),
      queryKey: commentQueryKeys.authors(normalizedAccountIds),
    };
  },
};
