import type { QueryClient } from "@tanstack/react-query";
import { commentApi } from "../../../../shared/api/moviesApi";
import { commentQueryKeys } from "./commentQueries";

export type CreateCommentVariables = {
  message: string;
  movieId: number;
};

export type UpdateCommentVariables = {
  commentId: number;
  message: string;
  movieId: number;
};

export type DeleteCommentVariables = {
  commentId: number;
  movieId: number;
};

const invalidateMovieComments = (
  queryClient: QueryClient | undefined,
  movieId: number,
) =>
  queryClient?.invalidateQueries({
    queryKey: commentQueryKeys.movie(movieId),
  });

export const createCommentMutationOptions = (queryClient?: QueryClient) => ({
  mutationFn: async ({ message, movieId }: CreateCommentVariables) => {
    const response = await commentApi.createComment(movieId, { message });
    return response.data;
  },
  onSuccess: (_data: unknown, variables: CreateCommentVariables) =>
    invalidateMovieComments(queryClient, variables.movieId),
});

export const updateCommentMutationOptions = (queryClient?: QueryClient) => ({
  mutationFn: async ({ commentId, message }: UpdateCommentVariables) => {
    const response = await commentApi.updateComment(commentId, { message });
    return response.data;
  },
  onSuccess: (_data: unknown, variables: UpdateCommentVariables) =>
    invalidateMovieComments(queryClient, variables.movieId),
});

export const deleteCommentMutationOptions = (queryClient?: QueryClient) => ({
  mutationFn: async ({ commentId }: DeleteCommentVariables) => {
    await commentApi.deleteComment(commentId);
  },
  onSuccess: (_data: unknown, variables: DeleteCommentVariables) =>
    invalidateMovieComments(queryClient, variables.movieId),
});
