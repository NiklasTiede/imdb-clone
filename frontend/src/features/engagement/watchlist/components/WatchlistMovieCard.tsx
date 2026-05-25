import CloseIcon from "@mui/icons-material/CloseSharp";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardActionArea from "@mui/material/CardActionArea";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import { Link } from "react-router";
import {
  posterHoverContainerSx,
  posterHoverTargetClassName,
  posterHoverTargetSx,
} from "../../../catalog";
import { MoviePosterImageSize, PosterImage } from "../../../../shared/media";
import { movieColors } from "../../../../theme";
import type { WatchlistItem } from "../model/watchlist";
import { formatMovieMeta, formatRelativeDate } from "../utils/watchlistFormat";

type WatchlistMovieCardProps = {
  item: WatchlistItem;
  onRemove: (movieId: number) => void;
};

const WatchlistMovieCard = ({ item, onRemove }: WatchlistMovieCardProps) => {
  const movie = item.movie;
  const movieId = item.movieId ?? movie?.id;
  const detailUrl = `/movie?id=${movieId}`;

  return (
    <Card
      sx={{
        backgroundColor: "transparent",
        boxShadow: "none",
        overflow: "visible",
        position: "relative",
        "&:hover .watchlist-remove": { opacity: 1 },
      }}
    >
      <CardActionArea
        aria-label={`Open ${movie?.primaryTitle ?? "movie"}`}
        component={Link}
        to={detailUrl}
        sx={{
          color: "text.primary",
          display: "block",
          textAlign: "left",
          textDecoration: "none",
          ...posterHoverContainerSx,
        }}
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
            posterImageToken={movie?.posterImageToken}
            size={MoviePosterImageSize.Medium}
            sx={{ height: "100%", objectFit: "cover", width: "100%" }}
          />
          {movie?.imdbRating !== undefined && (
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
              <StarIcon sx={{ color: movieColors.gold, fontSize: 14 }} />
              {movie.imdbRating}
            </Box>
          )}
        </Box>
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
          {movie?.primaryTitle ?? "Unknown title"}
        </Typography>
        <Typography sx={{ color: "text.secondary", fontSize: 12, mt: 0.25 }}>
          {movie ? formatMovieMeta(movie) : ""}
        </Typography>
        <Typography sx={{ color: "text.secondary", fontSize: 11, mt: 0.25 }}>
          {formatRelativeDate(item.addedAt)}
        </Typography>
      </CardActionArea>
      {movieId !== undefined && (
        <IconButton
          aria-label="Remove from watchlist"
          className="watchlist-remove"
          onClick={(event) => {
            event.preventDefault();
            event.stopPropagation();
            onRemove(movieId);
          }}
          size="small"
          sx={{
            backgroundColor: "rgba(0,0,0,0.7)",
            color: "common.white",
            opacity: { xs: 0.75, md: 0 },
            position: "absolute",
            right: 6,
            top: 6,
            transition: "opacity 150ms ease, background-color 150ms ease",
            zIndex: 1,
            "@media (hover: none)": {
              opacity: 0.75,
            },
            "&:hover": {
              backgroundColor: "error.dark",
              opacity: 1,
            },
          }}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      )}
    </Card>
  );
};

export default WatchlistMovieCard;
