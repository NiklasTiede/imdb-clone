import CloseIcon from "@mui/icons-material/CloseSharp";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import { PosterMovieCard } from "../../../catalog";
import type { RatedMovie } from "../api/ratingQueries";
import {
  imdbRatingBadgeSx,
  imdbRatingStarSx,
  yourRatingBadgeSx,
  yourRatingStarSx,
} from "./RatingsMovieCard.styles";

type RatingsMovieCardProps = {
  item: RatedMovie;
  onRemove?: ((movieId: number) => void) | undefined;
};

const RatingsMovieCard = ({ item, onRemove }: RatingsMovieCardProps) => {
  const movieId = item.movie.id;

  return (
    <PosterMovieCard
      action={
        movieId !== undefined && onRemove ? (
          <IconButton
            aria-label="Delete rating"
            className="movie-card-action"
            onClick={() => onRemove(movieId)}
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
      caption="Rated by you"
      movie={item.movie}
      posterOverlay={
        <>
          <Box aria-label={`Your rating ${item.rating} out of 10`} sx={yourRatingBadgeSx}>
            <StarIcon sx={yourRatingStarSx} />
            {item.rating}
          </Box>
          {item.movie.imdbRating !== undefined && (
            <Box sx={imdbRatingBadgeSx}>
              <StarIcon sx={imdbRatingStarSx} />
              {item.movie.imdbRating}
            </Box>
          )}
        </>
      }
      showImdbRating={false}
    />
  );
};

export default RatingsMovieCard;
