import { InfiniteData, QueryClient } from "@tanstack/react-query";
import { describe, expect, test, vi } from "vitest";
import type { RatingLibraryResponse } from "../../../../client/movies/generator-output";
import { ratingApi } from "../../../../shared/api/moviesApi";
import {
  rateMovieMutationOptions,
  removeRatingMutationOptions,
} from "./ratingMutations";

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

describe("rateMovieMutationOptions", () => {
  test("calls rateMovie with movieId and score for a positive score", async () => {
    const queryClient = makeQueryClient();
    const rateSpy = vi
      .spyOn(ratingApi, "rateMovie")
      .mockResolvedValue({} as never);
    const deleteSpy = vi
      .spyOn(ratingApi, "deleteRating")
      .mockResolvedValue({} as never);

    await queryClient
      .getMutationCache()
      .build(queryClient, rateMovieMutationOptions())
      .execute({ movieId: 5, score: 8 });

    expect(rateSpy).toHaveBeenCalledWith(5, 8);
    expect(deleteSpy).not.toHaveBeenCalled();

    rateSpy.mockRestore();
    deleteSpy.mockRestore();
  });

  test("calls deleteRating when the score is null (clear rating)", async () => {
    const queryClient = makeQueryClient();
    const rateSpy = vi
      .spyOn(ratingApi, "rateMovie")
      .mockResolvedValue({} as never);
    const deleteSpy = vi
      .spyOn(ratingApi, "deleteRating")
      .mockResolvedValue({} as never);

    await queryClient
      .getMutationCache()
      .build(queryClient, rateMovieMutationOptions())
      .execute({ movieId: 5, score: null });

    expect(deleteSpy).toHaveBeenCalledWith(5);
    expect(rateSpy).not.toHaveBeenCalled();

    rateSpy.mockRestore();
    deleteSpy.mockRestore();
  });

  test("optimistically removes a movie from the ratings library", async () => {
    const queryClient = makeQueryClient();
    const queryKey = ["rating", "library", "ada", "score_desc", 30] as const;
    const initial = ratingLibraryData();
    queryClient.setQueryData(queryKey, initial);
    const deleteSpy = vi
      .spyOn(ratingApi, "deleteRating")
      .mockResolvedValue({} as never);

    await queryClient
      .getMutationCache()
      .build(
        queryClient,
        removeRatingMutationOptions({
          onRemoveError: vi.fn(),
          queryClient,
          ratingQueryKey: queryKey,
        }),
      )
      .execute(5);

    expect(
      queryClient.getQueryData<InfiniteData<RatingLibraryResponse>>(queryKey)
        ?.pages[0]?.items?.content,
    ).toEqual([{ movie: { id: 7 }, movieId: 7, rating: 8 }]);
    expect(deleteSpy).toHaveBeenCalledWith(5);
    deleteSpy.mockRestore();
  });

  test("restores the ratings library when removal fails", async () => {
    const queryClient = makeQueryClient();
    const queryKey = ["rating", "library", "ada", "score_desc", 30] as const;
    const initial = ratingLibraryData();
    const onRemoveError = vi.fn();
    queryClient.setQueryData(queryKey, initial);
    const deleteSpy = vi
      .spyOn(ratingApi, "deleteRating")
      .mockRejectedValue(new Error("delete failed"));

    await expect(
      queryClient
        .getMutationCache()
        .build(
          queryClient,
          removeRatingMutationOptions({
            onRemoveError,
            queryClient,
            ratingQueryKey: queryKey,
          }),
        )
        .execute(5),
    ).rejects.toThrow("delete failed");

    expect(queryClient.getQueryData(queryKey)).toEqual(initial);
    expect(onRemoveError).toHaveBeenCalledOnce();
    deleteSpy.mockRestore();
  });
});

const ratingLibraryData = (): InfiniteData<RatingLibraryResponse> => ({
  pageParams: [0],
  pages: [
    {
      items: {
        content: [
          { movie: { id: 5 }, movieId: 5, rating: 9 },
          { movie: { id: 7 }, movieId: 7, rating: 8 },
        ],
        last: true,
        page: 0,
        size: 30,
        totalElements: 2,
        totalPages: 1,
      },
    },
  ],
});
