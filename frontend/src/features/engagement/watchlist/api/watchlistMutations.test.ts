import type { InfiniteData } from "@tanstack/react-query";
import { QueryClient } from "@tanstack/react-query";
import { describe, expect, test, vi } from "vitest";
import type { WatchlistLibraryResponse } from "../../../../client/movies/generator-output";
import { watchlistApi } from "../../../../shared/api/moviesApi";
import {
  removeFromWatchlistMutationOptions,
  toggleWatchlistMutationOptions,
} from "./watchlistMutations";

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

describe("removeFromWatchlistMutationOptions", () => {
  test("removes an item optimistically from every loaded library page", async () => {
    const queryClient = makeQueryClient();
    const queryKey = ["watchlist", "library", "nik", "addedAt_desc", 30] as const;
    const initial: InfiniteData<WatchlistLibraryResponse> = {
      pageParams: [0, 1],
      pages: [
        {
          insights: { totalMovies: 3 },
          items: {
            content: [{ movieId: 1 }, { movieId: 2 }],
            totalElements: 3,
          },
        },
        {
          items: {
            content: [{ movieId: 3 }],
            totalElements: 3,
          },
        },
      ],
    };
    queryClient.setQueryData(queryKey, initial);

    const options = removeFromWatchlistMutationOptions({
      onRemoveError: vi.fn(),
      onRemoved: vi.fn(),
      queryClient,
      watchlistQueryKey: queryKey,
    });

    await options.onMutate(2);

    expect(queryClient.getQueryData<InfiniteData<WatchlistLibraryResponse>>(queryKey))
      .toMatchObject({
        pages: [
          { items: { content: [{ movieId: 1 }], totalElements: 2 } },
          { items: { content: [{ movieId: 3 }], totalElements: 2 } },
        ],
      });
  });
});
