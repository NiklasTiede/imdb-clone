import { QueryClient } from "@tanstack/react-query";
import { describe, expect, test, vi } from "vitest";
import { ratingApi } from "../../../../shared/api/moviesApi";
import { rateMovieMutationOptions } from "./ratingMutations";

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
});
