import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Skeleton from "@mui/material/Skeleton";
import Snackbar from "@mui/material/Snackbar";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { Fragment, useMemo, useState } from "react";
import { RoleNameEnum, useAuthSessionSnapshot } from "../../../../shared/auth";
import {
  createCommentMutationOptions,
  deleteCommentMutationOptions,
  updateCommentMutationOptions,
} from "../api/commentMutations";
import { commentQueries } from "../api/commentQueries";
import type { CommentAuthor } from "../model/comment";
import CommentComposer from "./CommentComposer";
import CommentItem from "./CommentItem";

type MovieCommentsSectionProps = {
  movieId: number;
  movieTitle: string;
  onRequestSignIn: () => void;
};

const MovieCommentsSection = ({
  movieId,
  movieTitle,
  onRequestSignIn,
}: MovieCommentsSectionProps) => {
  const queryClient = useQueryClient();
  const { session } = useAuthSessionSnapshot();
  const [composerError, setComposerError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const commentsQuery = useInfiniteQuery(commentQueries.movie(movieId));
  const comments = useMemo(
    () => commentsQuery.data?.pages.flatMap((page) => page.content ?? []) ?? [],
    [commentsQuery.data],
  );
  const accountIds = useMemo(
    () =>
      [session?.id, ...comments.map((comment) => comment.accountId)]
        .filter((accountId): accountId is number => accountId !== undefined)
        .filter((accountId, index, all) => all.indexOf(accountId) === index),
    [comments, session?.id],
  );
  const authorsQuery = useQuery(commentQueries.authors(accountIds));
  const authorsById = useMemo(
    () =>
      new Map<number, CommentAuthor>(
        (authorsQuery.data ?? [])
          .filter((author) => author.id !== undefined)
          .map((author) => [author.id as number, author]),
      ),
    [authorsQuery.data],
  );
  const createComment = useMutation(createCommentMutationOptions(queryClient));
  const updateComment = useMutation(updateCommentMutationOptions(queryClient));
  const deleteComment = useMutation(deleteCommentMutationOptions(queryClient));
  const totalComments =
    commentsQuery.data?.pages[0]?.totalElements ?? comments.length;
  const isAdmin = session?.roles?.includes(RoleNameEnum.Admin) ?? false;

  const publishComment = async (message: string) => {
    try {
      setComposerError(null);
      await createComment.mutateAsync({ message, movieId });
      setSuccessMessage("Comment published.");
    } catch {
      setComposerError("Could not publish your comment. Please try again.");
      throw new Error("Could not publish comment");
    }
  };

  return (
    <Box
      aria-labelledby="movie-comments-title"
      component="section"
      data-testid="movie-comments"
      sx={{
        borderTop: "1px solid",
        borderColor: "divider",
        py: { xs: 3, md: 4 },
      }}
    >
      <Box sx={{ maxWidth: 880, mx: "auto" }}>
        <Stack
          direction="row"
          spacing={1.25}
          sx={{ alignItems: "center", flexWrap: "wrap" }}
        >
          <Typography
            component="h2"
            id="movie-comments-title"
            sx={{ fontSize: { xs: 21, sm: 24 }, fontWeight: 700 }}
          >
            Community
          </Typography>
          {!commentsQuery.isPending && (
            <Chip
              label={`${totalComments} ${totalComments === 1 ? "comment" : "comments"}`}
              size="small"
              variant="outlined"
            />
          )}
        </Stack>
        <Typography sx={{ color: "text.secondary", mt: 0.75, mb: 2.5 }}>
          Comments may contain spoilers.
        </Typography>

        <CommentComposer
          author={session?.id ? authorsById.get(session.id) : undefined}
          errorMessage={composerError}
          isAuthenticated={session !== null}
          isSubmitting={createComment.isPending}
          movieTitle={movieTitle}
          onRequestSignIn={onRequestSignIn}
          onSubmit={publishComment}
        />

        {commentsQuery.isPending ? (
          <Stack
            aria-label="Loading comments"
            role="status"
            spacing={2}
            sx={{ mt: 3 }}
          >
            {[0, 1, 2].map((item) => (
              <Stack direction="row" key={item} spacing={1.5}>
                <Skeleton height={40} variant="circular" width={40} />
                <Box sx={{ flex: 1 }}>
                  <Skeleton height={24} width="32%" />
                  <Skeleton height={20} width="100%" />
                  <Skeleton height={20} width="72%" />
                </Box>
              </Stack>
            ))}
          </Stack>
        ) : commentsQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => {
                  void commentsQuery.refetch();
                }}
              >
                Try again
              </Button>
            }
            severity="error"
            sx={{ mt: 3 }}
          >
            Could not load comments for this movie.
          </Alert>
        ) : comments.length === 0 ? (
          <Box sx={{ py: { xs: 5, sm: 6 }, textAlign: "center" }}>
            <ForumOutlinedIcon sx={{ color: "text.secondary", fontSize: 34 }} />
            <Typography sx={{ fontSize: 16, fontWeight: 700, mt: 1 }}>
              No comments yet
            </Typography>
            <Typography sx={{ color: "text.secondary", mt: 0.5 }}>
              Be the first to share what you thought about this movie.
            </Typography>
          </Box>
        ) : (
          <Box sx={{ mt: 2.5 }}>
            {comments.map((comment, index) => (
              <Fragment key={comment.id ?? `${comment.accountId}-${index}`}>
                <CommentItem
                  author={
                    comment.accountId
                      ? authorsById.get(comment.accountId)
                      : undefined
                  }
                  canManage={
                    isAdmin ||
                    (session?.id !== undefined &&
                      session.id === comment.accountId)
                  }
                  comment={comment}
                  isDeleting={
                    deleteComment.isPending &&
                    deleteComment.variables?.commentId === comment.id
                  }
                  isUpdating={
                    updateComment.isPending &&
                    updateComment.variables?.commentId === comment.id
                  }
                  onDelete={async (commentId) => {
                    await deleteComment.mutateAsync({ commentId, movieId });
                    setSuccessMessage("Comment deleted.");
                  }}
                  onUpdate={async (commentId, message) => {
                    await updateComment.mutateAsync({
                      commentId,
                      message,
                      movieId,
                    });
                    setSuccessMessage("Comment updated.");
                  }}
                />
                {index < comments.length - 1 && <Divider />}
              </Fragment>
            ))}

            {commentsQuery.hasNextPage && (
              <Box sx={{ display: "flex", justifyContent: "center", mt: 2 }}>
                <Button
                  disabled={commentsQuery.isFetchingNextPage}
                  onClick={() => {
                    void commentsQuery.fetchNextPage();
                  }}
                  variant="outlined"
                >
                  {commentsQuery.isFetchingNextPage ? (
                    <>
                      <CircularProgress size={16} sx={{ mr: 1 }} />
                      Loading...
                    </>
                  ) : (
                    "Load more comments"
                  )}
                </Button>
              </Box>
            )}
          </Box>
        )}
      </Box>

      <Snackbar
        anchorOrigin={{ horizontal: "center", vertical: "bottom" }}
        autoHideDuration={3500}
        onClose={() => setSuccessMessage(null)}
        open={successMessage !== null}
      >
        <Alert
          onClose={() => setSuccessMessage(null)}
          severity="success"
          variant="filled"
        >
          {successMessage ?? ""}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default MovieCommentsSection;
