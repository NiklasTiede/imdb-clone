import { describe, expect, test, vi } from "vitest";
import { accountApi, commentApi } from "../../../../shared/api/moviesApi";
import { commentQueries } from "./commentQueries";

describe("commentQueries", () => {
  test("loads numbered movie-comment pages", async () => {
    const getComments = vi
      .spyOn(commentApi, "getCommentsByMovieId")
      .mockResolvedValue({
        data: { content: [], last: false, page: 2 },
      } as never);

    const options = commentQueries.movie(42);
    const result = await options.queryFn({ pageParam: 2 });

    expect(options.queryKey).toEqual(["comments", "movie", 42]);
    expect(options.initialPageParam).toBe(0);
    expect(getComments).toHaveBeenCalledWith(42, 2, 10);
    expect(options.getNextPageParam(result)).toBe(3);
  });

  test("normalizes author ids and requests them in bounded batches", async () => {
    const getSummaries = vi
      .spyOn(accountApi, "getPublicAccountSummaries")
      .mockResolvedValue({ data: [] } as never);
    const accountIds = [
      2,
      2,
      ...Array.from({ length: 31 }, (_, index) => index + 1),
    ];

    const options = commentQueries.authors(accountIds);
    await options.queryFn();

    expect(options.queryKey[2]).toEqual(
      Array.from({ length: 31 }, (_, index) => index + 1),
    );
    expect(getSummaries).toHaveBeenCalledTimes(2);
    expect(getSummaries.mock.calls[0]?.[0]).toHaveLength(30);
    expect(getSummaries.mock.calls[1]?.[0]).toEqual([31]);
  });

  test("disables author loading without valid ids", () => {
    expect(commentQueries.authors([]).enabled).toBe(false);
    expect(commentQueries.authors([0, -1]).enabled).toBe(false);
  });
});
