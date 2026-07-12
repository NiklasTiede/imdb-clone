import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import CloseIcon from "@mui/icons-material/CloseSharp";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import type { MouseEvent, ReactNode } from "react";
import { Link as RouterLink } from "react-router";
import { ObjectStorageImageSize, PosterImage } from "../../../shared/media";
import { movieColors } from "../../../theme";
import { getMoviePosterToken, type Movie } from "../model/movie";

type MovieListRating = {
  label?: string;
  value?: number | null | undefined;
  variant: "imdb" | "user";
};

type MovieListAction = {
  ariaLabel: string;
  color?: "danger" | "default";
  icon: "bookmark" | "bookmark-filled" | "delete" | Exclude<ReactNode, string>;
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
  [
    movie.startYear,
    movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : undefined,
  ]
    .filter(Boolean)
    .join(" · ");

const ratingColor = {
  imdb: movieColors.gold,
  user: movieColors.info,
};

const ratingBackground = {
  imdb: "rgba(255,183,0,0.12)",
  user: "rgba(122,184,255,0.14)",
};

const defaultRatingLabel = (variant: MovieListRating["variant"]) =>
  variant === "user" ? "You" : "IMDb";

const MovieListRatingPill = ({
  rating,
}: {
  rating?: MovieListRating | undefined;
}) => {
  if (rating?.value === undefined || rating.value === null) {
    return null;
  }

  const label = rating.label ?? defaultRatingLabel(rating.variant);

  return (
    <Box
      aria-label={`${label} rating ${rating.value}`}
      component="span"
      sx={{
        alignItems: "center",
        backgroundColor: ratingBackground[rating.variant],
        border: "1px solid",
        borderColor:
          rating.variant === "user"
            ? "rgba(122,184,255,0.22)"
            : "rgba(255,183,0,0.18)",
        borderRadius: 1,
        color: ratingColor[rating.variant],
        display: "inline-flex",
        fontSize: 12,
        fontWeight: 800,
        gap: 0.35,
        justifyContent: "center",
        minHeight: 28,
        minWidth: 62,
        px: 0.75,
        whiteSpace: "nowrap",
      }}
    >
      <StarIcon sx={{ fontSize: 13 }} />
      <Box component="span">{rating.value}</Box>
      <Typography
        component="span"
        sx={{
          color: "inherit",
          fontSize: 10,
          fontWeight: 700,
          opacity: 0.8,
        }}
      >
        {label}
      </Typography>
    </Box>
  );
};

const GenrePill = ({
  children,
  hideOnMobile = false,
}: {
  children: string;
  hideOnMobile?: boolean;
}) => (
  <Box
    component="span"
    sx={{
      backgroundColor: "rgba(255,255,255,0.06)",
      border: "1px solid rgba(255,255,255,0.06)",
      borderRadius: 10,
      color: "rgba(255,255,255,0.7)",
      display: hideOnMobile ? { xs: "none", sm: "inline" } : "inline",
      fontSize: 10,
      lineHeight: "18px",
      px: 0.8,
      whiteSpace: "nowrap",
    }}
  >
    {children}
  </Box>
);

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
  const movieId = movie.id;
  const movieLink =
    to ?? (movieId === undefined ? "#" : `/movie?id=${movieId}`);
  const genres = Array.from(movie.movieGenre ?? [])
    .slice(0, 2)
    .map((genre) => formatEnum(String(genre)));
  const meta = formatMeta(movie);

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
        backgroundColor: "rgba(255,255,255,0.012)",
        border: "1px solid rgba(255,255,255,0.045)",
        borderRadius: 1,
        display: "grid",
        gap: { xs: 1, sm: 1.5 },
        gridTemplateColumns: "minmax(0, 1fr) auto",
        listStyle: "none",
        minHeight: { xs: 94, sm: 122 },
        px: { xs: 1, sm: 1.5 },
        py: { xs: 1, sm: 1.25 },
        transition: "background-color 140ms ease, border-color 140ms ease",
        "&:hover, &:focus-within": {
          backgroundColor: "rgba(255,255,255,0.04)",
          borderColor: "rgba(255,255,255,0.11)",
        },
        "&:hover .movie-list-row-action, &:focus-within .movie-list-row-action": {
          opacity: 1,
        },
      }}
    >
      <Box
        aria-label={movie.primaryTitle ?? "Unknown title"}
        component={RouterLink}
        to={movieLink}
        sx={{
          alignItems: { xs: "center", sm: "flex-start" },
          alignSelf: { xs: "center", sm: "flex-start" },
          borderRadius: 0.75,
          color: "inherit",
          display: "grid",
          gap: { xs: 1.1, sm: 1.5 },
          gridTemplateColumns: { xs: "48px minmax(0, 1fr)", sm: "68px minmax(0, 1fr)" },
          minWidth: 0,
          outline: "none",
          textDecoration: "none",
          "&:focus-visible": {
            outline: `2px solid ${movieColors.info}`,
            outlineOffset: 4,
          },
        }}
      >
        <PosterImage
          posterImageToken={getMoviePosterToken(movie)}
          size={ObjectStorageImageSize.Small}
          sx={{
            aspectRatio: "2 / 3",
            backgroundColor: movieColors.surfaceInset,
            border: "1px solid rgba(255,255,255,0.08)",
            borderRadius: 0.75,
            height: { xs: 72, sm: 102 },
            objectFit: "cover",
            width: { xs: 48, sm: 68 },
          }}
        />

        <Stack spacing={0.55} sx={{ minWidth: 0, pt: { sm: 0.2 } }}>
          <Typography
            sx={{
              WebkitBoxOrient: "vertical",
              WebkitLineClamp: 2,
              color: "text.primary",
              display: "-webkit-box",
              fontSize: { xs: 14, sm: 15 },
              fontWeight: 800,
              lineHeight: 1.35,
              overflow: "hidden",
            }}
          >
            {movie.primaryTitle ?? "Unknown title"}
          </Typography>

          {meta && (
            <Typography sx={{ color: "text.secondary", fontSize: 12, lineHeight: 1.25 }}>
              {meta}
            </Typography>
          )}

          {(genres.length > 0 || timestamp) && (
            <Stack
              direction="row"
              spacing={0.5}
              useFlexGap
              sx={{ alignItems: "center", flexWrap: "wrap", minHeight: 18 }}
            >
              {genres.map((genre, index) => (
                <GenrePill hideOnMobile={index > 0} key={genre}>
                  {genre}
                </GenrePill>
              ))}
              {timestamp && (
                <Typography sx={{ color: "rgba(255,255,255,0.52)", fontSize: 11, lineHeight: 1.2 }}>
                  {timestamp}
                </Typography>
              )}
            </Stack>
          )}
        </Stack>
      </Box>

      <Stack
        direction="row"
        spacing={0.65}
        sx={{
          alignItems: "flex-start",
          alignSelf: "center",
          justifySelf: "end",
        }}
      >
        <Stack direction={{ xs: "column", md: "row" }} spacing={0.45}>
          <MovieListRatingPill rating={primaryRating} />
          <MovieListRatingPill rating={secondaryRating} />
        </Stack>

        {action && (
          <Tooltip title={action.ariaLabel}>
            <IconButton
              aria-label={action.ariaLabel}
              className="movie-list-row-action"
              onClick={handleActionClick}
              size="small"
              sx={{
                backgroundColor: "rgba(255,255,255,0.06)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 1,
                color: "text.secondary",
                height: 30,
                opacity: 0.72,
                transition: "opacity 140ms ease, background-color 140ms ease, color 140ms ease",
                width: 30,
                "&:focus-visible": {
                  opacity: 1,
                  outline: `2px solid ${movieColors.info}`,
                  outlineOffset: 2,
                },
                "&:hover":
                  action.color === "danger"
                    ? {
                        backgroundColor: "rgba(248,113,113,0.15)",
                        color: "error.light",
                      }
                    : {
                        backgroundColor: "rgba(255,255,255,0.1)",
                      },
              }}
            >
              {renderActionIcon(action.icon)}
            </IconButton>
          </Tooltip>
        )}
      </Stack>
    </Box>
  );
};

export default MovieListRow;
