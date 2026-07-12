import { useInfiniteQuery, type InfiniteData } from "@tanstack/react-query";
import type {
  HomeFeedRequest,
  HomeFeedResponse,
  MovieRecord,
} from "../../../client/movies/generator-output";
import { recommendationApi } from "../../../shared/api/moviesApi";

type HomeFeedPageParam = {
  cursor?: string;
  excludedMovieIds: number[];
  seed?: string;
};

const initialPageParam: HomeFeedPageParam = {
  excludedMovieIds: [],
};

const getShownMovieIds = (pages: HomeFeedResponse[]) =>
  Array.from(
    new Set(
      pages.flatMap((page) => [
        page.featuredMovie?.id,
        ...(page.featuredMovies ?? []).map((movie) => movie.id),
        ...(page.sections ?? []).flatMap((section) =>
          (section.items ?? []).map((item) => item.movie?.id),
        ),
      ]),
    ),
  ).filter((movieId): movieId is number => movieId !== undefined);

export const homeFeedQueryKey = (feedInstanceId: string) =>
  ["home", "feed", feedInstanceId] as const;

export const useHomeFeed = (feedInstanceId: string) =>
  useInfiniteQuery<
    HomeFeedResponse,
    Error,
    InfiniteData<HomeFeedResponse>,
    ReturnType<typeof homeFeedQueryKey>,
    HomeFeedPageParam
  >({
    gcTime: 1000 * 60 * 45,
    getNextPageParam: (lastPage, allPages): HomeFeedPageParam | undefined => {
      if (lastPage.exhausted || !lastPage.nextCursor) {
        return undefined;
      }

      const nextPageParam: HomeFeedPageParam = {
        cursor: lastPage.nextCursor,
        excludedMovieIds: getShownMovieIds(allPages),
      };
      if (lastPage.seed) {
        nextPageParam.seed = lastPage.seed;
      }
      return nextPageParam;
    },
    initialPageParam,
    queryFn: async ({ pageParam }) => {
      const request: HomeFeedRequest = {
        excludedMovieIds: pageParam.excludedMovieIds,
        feedInstanceId,
      };
      if (pageParam.cursor) {
        request.cursor = pageParam.cursor;
      }
      if (pageParam.seed) {
        request.seed = pageParam.seed;
      }
      return (await recommendationApi.homeFeed(request)).data;
    },
    queryKey: homeFeedQueryKey(feedInstanceId),
    staleTime: Infinity,
  });

export const getFeedMovies = (response: HomeFeedResponse): MovieRecord[] =>
  (response.sections ?? []).flatMap((section) =>
    (section.items ?? [])
      .map((item) => item.movie)
      .filter((movie): movie is MovieRecord => movie !== undefined),
  );
