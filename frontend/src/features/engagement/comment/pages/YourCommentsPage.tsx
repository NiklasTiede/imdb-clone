import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Pagination from "@mui/material/Pagination";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link as RouterLink } from "react-router";
import { useMemo, useState } from "react";
import { getUsername } from "../../../../shared/auth";
import { accountEngagementApi, moviesApi } from "../../../../shared/api/moviesApi";
import { accountQueries } from "../../../../shared/api/accountProfileQueries";
import PageContent from "../../../../shared/layout/PageContent";
import PageHeader from "../../../../shared/layout/PageHeader";
import { MoviePosterImageSize, PosterImage } from "../../../../shared/media";
import type { Movie } from "../../../catalog";
import { deleteCommentMutationOptions, updateCommentMutationOptions } from "../api/commentMutations";
import { commentQueryKeys } from "../api/commentQueries";
import CommentItem from "../components/CommentItem";

const PAGE_SIZE = 20;

const YourCommentsPage = () => {
  const username = getUsername();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const { data: currentProfile } = useQuery({
    ...accountQueries.currentProfile(),
    enabled: Boolean(username),
  });
  const query = useQuery({
    enabled: Boolean(username),
    queryFn: async () =>
      (await accountEngagementApi.getCommentsByAccount(username as string, page, PAGE_SIZE)).data,
    queryKey: commentQueryKeys.currentUser(username ?? "", page, PAGE_SIZE),
  });
  const update = useMutation(updateCommentMutationOptions(queryClient));
  const remove = useMutation(deleteCommentMutationOptions(queryClient));
  const comments = useMemo(() => query.data?.content ?? [], [query.data?.content]);
  const commentMovieIds = useMemo(
    () => [...new Set(comments.map((comment) => comment.movieId))].filter(
      (movieId): movieId is number => movieId !== undefined,
    ),
    [comments],
  );
  const commentMoviesQuery = useQuery({
    enabled: commentMovieIds.length > 0,
    queryFn: async () =>
      (await moviesApi.getMoviesByIds({ movieIds: commentMovieIds }, 0, commentMovieIds.length))
        .data.content ?? [],
    queryKey: ["catalog", "movies", "comment-context", commentMovieIds] as const,
  });
  const moviesById = useMemo(
    () => new Map(
      (commentMoviesQuery.data ?? []).flatMap((movie) =>
        movie.id === undefined ? [] : [[movie.id, movie] as const],
      ),
    ),
    [commentMoviesQuery.data],
  );

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
        {comments.map((comment) => {
          const movie = comment.movieId === undefined ? undefined : moviesById.get(comment.movieId);

          return (
            <Stack key={comment.id} spacing={0.75} sx={{ borderBottom: "1px solid", borderColor: "divider" }}>
              {comment.movieId !== undefined && <CommentMovieContext commentId={comment.id} movie={movie} movieId={comment.movieId} />}
              <CommentItem
                author={
                  username
                    ? {
                        ...(currentProfile?.imageUrlToken
                          ? { imageUrlToken: currentProfile.imageUrlToken }
                          : {}),
                        username,
                      }
                    : undefined
                }
                canManage
                comment={comment}
                isDeleting={remove.isPending}
                isUpdating={update.isPending}
                onDelete={(commentId) => deleteComment(commentId, comment.movieId)}
                onUpdate={(commentId, message) =>
                  updateComment(commentId, message, comment.movieId)
                }
              />
            </Stack>
          );
        })}
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

const CommentMovieContext = ({
  commentId,
  movie,
  movieId,
}: {
  commentId?: number | undefined;
  movie?: Movie | undefined;
  movieId: number;
}) => {
  const title = movie?.primaryTitle?.trim() || `Movie #${movieId}`;
  const details = [movie?.startYear, movie?.runtimeMinutes ? `${movie.runtimeMinutes} min` : undefined]
    .filter((value): value is string | number => value !== undefined)
    .join(" · ");

  return (
    <Box
      aria-label={`Open ${title} discussion`}
      component={RouterLink}
      sx={{
        alignItems: "center",
        borderRadius: 1,
        color: "inherit",
        display: "flex",
        gap: 1.25,
        mt: 1.5,
        px: 1,
        py: 0.75,
        textDecoration: "none",
        transition: "background-color 150ms ease",
        "&:hover": { backgroundColor: "action.hover" },
        "&:focus-visible": { outline: "2px solid", outlineColor: "primary.main", outlineOffset: 2 },
      }}
      to={`/movie?id=${movieId}#comment-${commentId}`}
    >
      <PosterImage
        alt=""
        posterImageToken={movie?.posterImageToken}
        size={MoviePosterImageSize.Small}
        sx={{ borderRadius: 0.75, flex: "0 0 auto", height: 52, objectFit: "cover", width: 35 }}
      />
      <Box sx={{ minWidth: 0 }}>
        <Typography sx={{ color: "text.secondary", fontSize: 11, fontWeight: 700, letterSpacing: 0.55, textTransform: "uppercase" }}>
          Your comment on
        </Typography>
        <Typography noWrap sx={{ fontSize: 15, fontWeight: 800, lineHeight: 1.35 }}>
          {title}
        </Typography>
        {details && <Typography sx={{ color: "text.secondary", fontSize: 12, mt: 0.15 }}>{details}</Typography>}
      </Box>
    </Box>
  );
};

export default YourCommentsPage;
