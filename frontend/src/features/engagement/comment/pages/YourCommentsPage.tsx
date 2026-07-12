import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Pagination from "@mui/material/Pagination";
import Stack from "@mui/material/Stack";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link as RouterLink } from "react-router";
import { useState } from "react";
import { getUsername } from "../../../../shared/auth";
import { accountEngagementApi } from "../../../../shared/api/moviesApi";
import PageContent from "../../../../shared/layout/PageContent";
import PageHeader from "../../../../shared/layout/PageHeader";
import { deleteCommentMutationOptions, updateCommentMutationOptions } from "../api/commentMutations";
import { commentQueryKeys } from "../api/commentQueries";
import CommentItem from "../components/CommentItem";

const PAGE_SIZE = 20;

const YourCommentsPage = () => {
  const username = getUsername();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const query = useQuery({
    enabled: Boolean(username),
    queryFn: async () =>
      (await accountEngagementApi.getCommentsByAccount(username as string, page, PAGE_SIZE)).data,
    queryKey: commentQueryKeys.currentUser(username ?? "", page, PAGE_SIZE),
  });
  const update = useMutation(updateCommentMutationOptions(queryClient));
  const remove = useMutation(deleteCommentMutationOptions(queryClient));
  const comments = query.data?.content ?? [];

  const invalidateYourComments = async () => {
    if (username) {
      await queryClient.invalidateQueries({
        queryKey: commentQueryKeys.currentUserAllPages(username),
      });
    }
  };

  const deleteComment = async (commentId: number, movieId?: number) => {
    if (!movieId) {
      throw new Error("The comment is missing its movie reference.");
    }
    await remove.mutateAsync({ commentId, movieId });
    await invalidateYourComments();
  };

  const updateComment = async (
    commentId: number,
    message: string,
    movieId?: number,
  ) => {
    if (!movieId) {
      throw new Error("The comment is missing its movie reference.");
    }
    await update.mutateAsync({ commentId, message, movieId });
    await invalidateYourComments();
  };

  return (
    <PageContent maxWidth="900px">
      <Stack spacing={2.5}>
        <PageHeader eyebrow="Community" title="Your comments" subtitle="Review, edit, or remove what you have shared with the community." />
        {query.isLoading && <CircularProgress aria-label="Loading your comments" />}
        {query.isError && <Alert severity="error">Could not load your comments. Please try again.</Alert>}
        {!query.isLoading && !query.isError && comments.length === 0 && (
          <Alert action={<Button component={RouterLink} to="/" color="inherit" size="small">Browse movies</Button>} severity="info">You have not published any comments yet.</Alert>
        )}
        {comments.map((comment) => (
          <Stack key={comment.id} spacing={0.75} sx={{ borderBottom: "1px solid", borderColor: "divider" }}>
            <CommentItem
              author={username ? { username } : undefined}
              canManage
              comment={comment}
              isDeleting={remove.isPending}
              isUpdating={update.isPending}
              onDelete={(commentId) => deleteComment(commentId, comment.movieId)}
              onUpdate={(commentId, message) =>
                updateComment(commentId, message, comment.movieId)
              }
            />
            {comment.movieId && (
              <Button
                component={RouterLink}
                size="small"
                sx={{ alignSelf: "flex-start", mb: 1, textTransform: "none" }}
                to={`/movie?id=${comment.movieId}#comment-${comment.id}`}
              >
                View movie discussion
              </Button>
            )}
          </Stack>
        ))}
        {(query.data?.totalPages ?? 0) > 1 && (
          <Stack
            sx={{ alignItems: "center", borderTop: "1px solid", borderColor: "divider", pt: 2.5 }}
          >
            <Pagination
              count={query.data?.totalPages}
              onChange={(_event, nextPage) => setPage(nextPage - 1)}
              page={page + 1}
              shape="rounded"
              siblingCount={1}
              sx={{
                "& .MuiPaginationItem-root": { borderColor: "rgba(255,255,255,0.12)", color: "text.secondary" },
                "& .Mui-selected": { backgroundColor: "rgba(245,197,24,0.16)", color: "text.primary" },
              }}
              variant="outlined"
            />
          </Stack>
        )}
      </Stack>
    </PageContent>
  );
};

export default YourCommentsPage;
