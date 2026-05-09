import { QueryClient } from "@tanstack/react-query";
import { describe, expect, test, vi } from "vitest";
import { watchlistApi } from "../../../shared/api/moviesApi";
import { toggleWatchlistMutationOptions } from "./watchlistMutations";

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

describe("toggleWatchlistMutationOptions", () => {
  test("calls watchMovie when the movie is not yet bookmarked", async () => {
    const queryClient = makeQueryClient();
    const watchSpy = vi
      .spyOn(watchlistApi, "watchMovie")
      .mockResolvedValue({} as never);
    const deleteSpy = vi
      .spyOn(watchlistApi, "deleteWatchedMovie")
      .mockResolvedValue({} as never);

    await queryClient
      .getMutationCache()
      .build(queryClient, toggleWatchlistMutationOptions())
      .execute({ movieId: 42, isBookmarked: false });

    expect(watchSpy).toHaveBeenCalledWith(42);
    expect(deleteSpy).not.toHaveBeenCalled();

    watchSpy.mockRestore();
    deleteSpy.mockRestore();
  });

  test("calls deleteWatchedMovie when the movie is already bookmarked", async () => {
    const queryClient = makeQueryClient();
    const watchSpy = vi
      .spyOn(watchlistApi, "watchMovie")
      .mockResolvedValue({} as never);
    const deleteSpy = vi
      .spyOn(watchlistApi, "deleteWatchedMovie")
      .mockResolvedValue({} as never);

    await queryClient
      .getMutationCache()
      .build(queryClient, toggleWatchlistMutationOptions())
      .execute({ movieId: 42, isBookmarked: true });

    expect(deleteSpy).toHaveBeenCalledWith(42);
    expect(watchSpy).not.toHaveBeenCalled();

    watchSpy.mockRestore();
    deleteSpy.mockRestore();
  });
});
