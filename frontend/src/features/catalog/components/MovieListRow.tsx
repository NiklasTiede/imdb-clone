import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import CloseIcon from "@mui/icons-material/CloseSharp";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { MouseEvent, ReactNode } from "react";
import { Link as RouterLink } from "react-router";
import { ObjectStorageImageSize, PosterImage } from "../../../shared/media";
import { movieColors } from "../../../theme";
import { getMoviePosterToken, type Movie } from "../model/movie";
import { useMovieListLayout } from "./MovieListView";

type MovieListRating = {
  value?: number | null | undefined;
  variant: "imdb" | "user";
};

type MovieListAction = {
  ariaLabel: string;
  color?: "danger" | "default";
  icon: "bookmark" | "bookmark-filled" | "delete" | ReactNode;
  onClick: () => void;
};

type MovieListRowProps = {
  action?: MovieListAction | undefined;
  movie: Movie;
  primaryRating?: MovieListRating;
  secondaryRating?: MovieListRating;
  timestamp?: string;
  to?: string;
};

const formatEnum = (value?: string): string =>
  value
    ? value
        .toLowerCase()
        .split("_")
        .filter(Boolean)
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(" ")
    : "";

const formatMeta = (movie: Movie): string =>
  [movie.startYear, formatEnum(movie.movieType)].filter(Boolean).join(" · ");

const ratingColor = {
  imdb: movieColors.gold,
  user: movieColors.info,
};

const ratingBackground = {
  imdb: "rgba(255,183,0,0.12)",
  user: "rgba(77,171,247,0.15)",
};

const MovieListRatingPill = ({
  rating,
}: {
  rating?: MovieListRating | undefined;
}) => {
  if (rating?.value === undefined || rating.value === null) {
    return null;
  }

  return (
    <Box
      aria-label={`${rating.variant === "user" ? "User" : "IMDb"} rating ${rating.value}`}
      component="span"
      sx={{
        alignItems: "center",
        backgroundColor: ratingBackground[rating.variant],
        borderRadius: 1,
        color: ratingColor[rating.variant],
        display: "inline-flex",
        fontSize: rating.variant === "user" ? 13 : 12,
        fontWeight: rating.variant === "user" ? 700 : 600,
        gap: 0.35,
        justifyContent: "center",
        minWidth: rating.variant === "user" ? 72 : 62,
        px: 1,
        py: 0.45,
        whiteSpace: "nowrap",
      }}
    >
      <StarIcon sx={{ fontSize: 14 }} />
      {rating.value}
    </Box>
  );
};

const renderActionIcon = (icon: MovieListAction["icon"]) => {
  if (icon === "delete") {
    return <CloseIcon fontSize="small" />;
  }
  if (icon === "bookmark") {
    return <BookmarkBorderIcon fontSize="small" />;
  }
  if (icon === "bookmark-filled") {
    return <BookmarkIcon fontSize="small" />;
  }
  return icon;
};

const MovieListRow = ({
  action,
  movie,
  primaryRating,
  secondaryRating,
  timestamp,
  to,
}: MovieListRowProps) => {
  const {
    columns,
    gridTemplateColumns,
    hasRowActions,
    rowGridTemplateColumns,
  } = useMovieListLayout();
  const movieId = movie.id;
  const movieLink =
    to ?? (movieId === undefined ? "#" : `/movie?id=${movieId}`);
  const genres = Array.from(movie.movieGenre ?? []).slice(0, 3);

  const handleActionClick = (event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    action?.onClick();
  };

  return (
    <Box
      component="li"
      sx={{
        alignItems: "center",
        borderRadius: 1,
        display: "grid",
        gap: { xs: 1.25, md: 1.75 },
        gridTemplateColumns: rowGridTemplateColumns,
        listStyle: "none",
        minHeight: { xs: 76, md: 96 },
        px: { xs: 1.25, sm: 1.75 },
        py: { xs: 1, md: 1.25 },
        transition: "background-color 120ms ease",
        "&:hover": { backgroundColor: "rgba(255,255,255,0.035)" },
        "&:hover .movie-list-row-action": {
          opacity: 1,
        },
      }}
    >
      <Box
        aria-label={movie.primaryTitle ?? "Unknown title"}
        className="movie-list-row-link"
        component={RouterLink}
        to={movieLink}
        sx={{
          alignItems: "center",
          color: "inherit",
          display: "grid",
          gap: { xs: 1.25, md: 1.75 },
          gridColumn: hasRowActions ? "1 / -2" : "1 / -1",
          gridTemplateColumns,
          minWidth: 0,
          textDecoration: "none",
        }}
      >
        <PosterImage
          posterImageToken={getMoviePosterToken(movie)}
          size={ObjectStorageImageSize.Small}
          sx={{
            aspectRatio: "2 / 3",
            backgroundColor: movieColors.surfaceInset,
            border: "1px solid rgba(255,255,255,0.06)",
            borderRadius: 0.75,
            height: { xs: 66, md: 78 },
            objectFit: "cover",
            width: { xs: 44, md: 52 },
          }}
        />

        <Box sx={{ minWidth: 0 }}>
          <Typography
            sx={{
              color: "text.primary",
              fontSize: 14,
              fontWeight: 700,
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {movie.primaryTitle ?? "Unknown title"}
          </Typography>
          <Typography sx={{ color: "text.secondary", fontSize: 12, mt: 0.25 }}>
            {formatMeta(movie)}
          </Typography>
        </Box>

        {columns.genre && (
          <Stack
            direction="row"
            spacing={0.5}
            useFlexGap
            sx={{
              display: { xs: "none", md: "flex" },
              flexWrap: "wrap",
              maxHeight: 22,
              overflow: "hidden",
            }}
          >
            {genres.map((genre) => (
              <Box
                component="span"
                key={String(genre)}
                sx={{
                  backgroundColor: "rgba(255,255,255,0.06)",
                  borderRadius: 10,
                  color: "rgba(255,255,255,0.72)",
                  fontSize: 11,
                  lineHeight: "18px",
                  px: 1,
                  whiteSpace: "nowrap",
                }}
              >
                {formatEnum(String(genre))}
              </Box>
            ))}
          </Stack>
        )}

        {columns.runtime && (
          <Typography
            sx={{
              color: "text.secondary",
              display: { xs: "none", md: "block" },
              fontSize: 12,
              textAlign: "right",
              whiteSpace: "nowrap",
            }}
          >
            {movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : ""}
          </Typography>
        )}

        <Box sx={{ justifySelf: "end" }}>
          <MovieListRatingPill rating={primaryRating} />
        </Box>

        {columns.secondaryRating && (
          <Box sx={{ justifySelf: "end" }}>
            <MovieListRatingPill rating={secondaryRating} />
          </Box>
        )}

        {columns.timestamp && (
          <Typography
            sx={{
              color: "rgba(255,255,255,0.5)",
              display: { xs: "none", md: "block" },
              fontSize: 11,
              textAlign: "right",
              whiteSpace: "nowrap",
            }}
          >
            {timestamp}
          </Typography>
        )}
      </Box>

      {action ? (
        <IconButton
          aria-label={action.ariaLabel}
          className="movie-list-row-action"
          onClick={handleActionClick}
          size="small"
          sx={{
            backgroundColor: "rgba(255,255,255,0.06)",
            borderRadius: 1,
            color: "text.secondary",
            height: 28,
            opacity: 0,
            justifySelf: "end",
            transition: "opacity 120ms ease, background-color 120ms ease",
            width: 28,
            "&:hover":
              action.color === "danger"
                ? {
                    backgroundColor: "rgba(248,113,113,0.15)",
                    color: "error.light",
                  }
                : {
                    backgroundColor: "rgba(255,255,255,0.1)",
                  },
            "@media (hover: none)": {
              opacity: 0.72,
            },
          }}
        >
          {renderActionIcon(action.icon)}
        </IconButton>
      ) : (
        hasRowActions && <Box aria-hidden />
      )}
    </Box>
  );
};

export default MovieListRow;
