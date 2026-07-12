import BookmarkIcon from "@mui/icons-material/Bookmark";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardActionArea from "@mui/material/CardActionArea";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
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
  showTitle?: boolean;
};

const MovieCard = ({
  movie,
  isBookmarked = false,
  onOpen,
  onToggleBookmark,
  showTitle = true,
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
          "&:hover .movie-card-bookmark": {
            opacity: 1,
          },
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

          {showBookmark && (
            <IconButton
              aria-label={
                isBookmarked ? "Remove from watchlist" : "Add to watchlist"
              }
              className="movie-card-bookmark"
              size="small"
              onClick={(event) => {
                event.preventDefault();
                event.stopPropagation();
                onToggleBookmark(movie.id as number);
              }}
              sx={{
                backgroundColor: isBookmarked
                  ? "success.main"
                  : "rgba(0,0,0,0.65)",
                color: "common.white",
                opacity: isBookmarked ? 1 : 0,
                position: "absolute",
                right: 0.75,
                top: 0.75,
                transition: "opacity 150ms ease, background-color 150ms ease",
                "&:hover": {
                  backgroundColor: isBookmarked
                    ? "success.dark"
                    : "rgba(0,0,0,0.78)",
                },
              }}
            >
              {isBookmarked ? (
                <BookmarkIcon fontSize="small" />
              ) : (
                <BookmarkBorderIcon fontSize="small" />
              )}
            </IconButton>
          )}

          {movie.imdbRating !== undefined && (
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
        </Box>

        {showTitle && (
          <Typography
            sx={{
              display: "-webkit-box",
              fontSize: 13,
              fontWeight: 600,
              lineHeight: 1.25,
              mt: 1,
              overflow: "hidden",
              WebkitBoxOrient: "vertical",
              WebkitLineClamp: 2,
            }}
          >
            {title}
          </Typography>
        )}
        <Typography
          sx={{
            color: showTitle ? "text.secondary" : "text.disabled",
            fontSize: showTitle ? 12 : 11,
            fontWeight: showTitle ? 400 : 600,
            letterSpacing: showTitle ? 0 : "0.035em",
            minHeight: 18,
            mt: showTitle ? 0.25 : 0.45,
            px: showTitle ? 0 : 0.5,
            textAlign: showTitle ? "left" : "center",
          }}
        >
          {meta}
        </Typography>
      </CardActionArea>
    </Card>
  );
};

export const formatMovieMeta = (movie: Movie): string =>
  [movie.startYear, movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : null]
    .filter(Boolean)
    .join(" · ");

export default MovieCard;
