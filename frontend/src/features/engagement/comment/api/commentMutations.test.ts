import { QueryClient } from "@tanstack/react-query";
import { describe, expect, test, vi } from "vitest";
import { commentApi } from "../../../../shared/api/moviesApi";
import {
  createCommentMutationOptions,
  deleteCommentMutationOptions,
  updateCommentMutationOptions,
} from "./commentMutations";

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false },
    },
  });

describe("comment mutation options", () => {
  test("creates, updates, and deletes comments through the generated client", async () => {
    const queryClient = makeQueryClient();
    const create = vi
      .spyOn(commentApi, "createComment")
      .mockResolvedValue({ data: { id: 9 } } as never);
    const update = vi
      .spyOn(commentApi, "updateComment")
      .mockResolvedValue({ data: { id: 9 } } as never);
    const remove = vi
      .spyOn(commentApi, "deleteComment")
      .mockResolvedValue({} as never);

    await queryClient
      .getMutationCache()
      .build(queryClient, createCommentMutationOptions(queryClient))
      .execute({ message: "A thoughtful comment", movieId: 4 });
    await queryClient
      .getMutationCache()
      .build(queryClient, updateCommentMutationOptions(queryClient))
      .execute({ commentId: 9, message: "Updated", movieId: 4 });
    await queryClient
      .getMutationCache()
      .build(queryClient, deleteCommentMutationOptions(queryClient))
      .execute({ commentId: 9, movieId: 4 });

    expect(create).toHaveBeenCalledWith(4, { message: "A thoughtful comment" });
    expect(update).toHaveBeenCalledWith(9, { message: "Updated" });
    expect(remove).toHaveBeenCalledWith(9);
  });
});
