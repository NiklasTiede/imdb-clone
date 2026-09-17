import CloseIcon from "@mui/icons-material/CloseSharp";
import IconButton from "@mui/material/IconButton";
import { PosterMovieCard, type Movie } from "../../../catalog";
import type { WatchlistItem } from "../model/watchlist";
import { formatRelativeDate } from "../utils/watchlistFormat";
import { scrimTint } from "../../../../theme";

type WatchlistMovieCardProps = {
  item: WatchlistItem;
  onRemove: (movieId: number) => void;
};

const WatchlistMovieCard = ({ item, onRemove }: WatchlistMovieCardProps) => {
  const movieId = item.movieId ?? item.movie?.id;
  const movie: Movie = {
    ...item.movie,
    primaryTitle: item.movie?.primaryTitle ?? "Unknown title",
  };
  if (movieId !== undefined) {
    movie.id = movieId;
  }

  return (
    <PosterMovieCard
      action={
        movieId !== undefined ? (
          <IconButton
            aria-label="Remove from watchlist"
            className="movie-card-action"
            onClick={() => onRemove(movieId)}
            size="small"
            sx={{
              backgroundColor: scrimTint(0.72),
              color: "text.primary",
              opacity: { xs: 0.75, md: 0 },
              position: "absolute",
              right: 6,
              top: 6,
              transition: "opacity 150ms ease, background-color 150ms ease",
              zIndex: 1,
              "@media (hover: none)": { opacity: 0.75 },
              "&:hover": {
                backgroundColor: "error.dark",
                opacity: 1,
              },
            }}
          >
            <CloseIcon fontSize="small" />
          </IconButton>
        ) : null
      }
      caption={formatRelativeDate(item.addedAt)}
      movie={movie}
    />
  );
};

export default WatchlistMovieCard;
