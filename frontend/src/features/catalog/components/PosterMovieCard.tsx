import BookmarkIcon from "@mui/icons-material/Bookmark";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardActionArea from "@mui/material/CardActionArea";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";
import { Link } from "react-router";
import { getMoviePosterToken, type Movie } from "../model/movie";
import { IMDB_GOLD } from "./RatingPill";
import {
  posterHoverContainerSx,
  posterHoverTargetClassName,
  posterHoverTargetSx,
} from "./posterHover";
import { MoviePosterImageSize, PosterImage } from "../../../shared/media";

type MovieCardProps = {
  movie: Movie;
  isBookmarked?: boolean;
  onOpen?: ((movieId: number) => void) | undefined;
  onToggleBookmark?: ((movieId: number) => void) | undefined;
  action?: ReactNode;
  caption?: string;
  posterOverlay?: ReactNode;
  showImdbRating?: boolean;
};

const MovieCard = ({
  movie,
  isBookmarked = false,
  onOpen,
  onToggleBookmark,
  action,
  caption,
  posterOverlay,
  showImdbRating = true,
}: MovieCardProps) => {
  const detailUrl = `/movie?id=${movie.id ?? ""}`;
  const title = movie.primaryTitle ?? "Untitled movie";
  const meta = formatMovieMeta(movie);
  const showBookmark = onToggleBookmark && movie.id !== undefined;

  return (
    <Card
      sx={{
        backgroundColor: "transparent",
        boxShadow: "none",
        overflow: "visible",
        position: "relative",
        "&:hover .movie-card-action": { opacity: 1 },
      }}
    >
      <CardActionArea
        aria-label={[title, meta].filter(Boolean).join(", ")}
        component={Link}
        onClick={() => {
          if (movie.id !== undefined) {
            onOpen?.(movie.id);
          }
        }}
        to={detailUrl}
        sx={{
          color: "text.primary",
          display: "block",
          textAlign: "left",
          textDecoration: "none",
          ...posterHoverContainerSx,
          "& .MuiCardActionArea-focusHighlight": { display: "none" },
        }}
        title={title}
      >
        <Box
          className={posterHoverTargetClassName}
          sx={{
            aspectRatio: "2 / 3",
            backgroundColor: "background.paper",
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 1,
            overflow: "hidden",
            position: "relative",
            ...posterHoverTargetSx,
          }}
        >
          <PosterImage
            posterImageToken={getMoviePosterToken(movie)}
            size={MoviePosterImageSize.Medium}
            sx={{
              height: "100%",
              objectFit: "cover",
              width: "100%",
            }}
          />

          {showImdbRating && movie.imdbRating !== undefined && (
            <Box
              sx={{
                alignItems: "center",
                backgroundColor: "rgba(0,0,0,0.75)",
                borderRadius: 0.75,
                bottom: 6,
                color: "common.white",
                display: "inline-flex",
                fontSize: 12,
                fontWeight: 600,
                gap: 0.25,
                left: 6,
                px: 0.75,
                py: 0.25,
                position: "absolute",
              }}
            >
              <StarIcon sx={{ color: IMDB_GOLD, fontSize: 14 }} />
              {movie.imdbRating.toFixed(1)}
            </Box>
          )}
          {posterOverlay}
        </Box>

        <Typography
          sx={{
            color: "text.disabled",
            fontSize: 11,
            fontWeight: 600,
            letterSpacing: "0.035em",
            minHeight: 18,
            mt: 0.45,
            px: 0.5,
            textAlign: "center",
          }}
        >
          {meta}
        </Typography>
        {caption && (
          <Typography
            sx={{
              color: "text.secondary",
              fontSize: 11,
              mt: 0.1,
              textAlign: "center",
            }}
          >
            {caption}
          </Typography>
        )}
      </CardActionArea>
      {showBookmark && (
        <IconButton
          aria-label={isBookmarked ? "Remove from watchlist" : "Add to watchlist"}
          className="movie-card-action"
          size="small"
          onClick={() => onToggleBookmark(movie.id as number)}
          sx={{
            backgroundColor: isBookmarked ? "success.main" : "rgba(0,0,0,0.65)",
            color: "common.white",
            opacity: isBookmarked ? 1 : 0,
            position: "absolute",
            right: 0.75,
            top: 0.75,
            transition: "opacity 150ms ease, background-color 150ms ease",
            "@media (hover: none)": { opacity: isBookmarked ? 1 : 0.75 },
            "&:hover": {
              backgroundColor: isBookmarked ? "success.dark" : "rgba(0,0,0,0.78)",
            },
          }}
        >
          {isBookmarked ? <BookmarkIcon fontSize="small" /> : <BookmarkBorderIcon fontSize="small" />}
        </IconButton>
      )}
      {action}
    </Card>
  );
};

export const formatMovieMeta = (movie: Movie): string =>
  [movie.startYear, movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : null]
    .filter(Boolean)
    .join(" · ");

export default MovieCard;
