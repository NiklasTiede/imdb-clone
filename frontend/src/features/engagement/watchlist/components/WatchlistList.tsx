import CloseIcon from "@mui/icons-material/CloseSharp";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Link } from "react-router";
import { WatchedMovieRecord } from "../../../../client/movies/generator-output";
import { MinioImageSize, PosterImage } from "../../../../shared/media";
import { movieColors } from "../../../../theme";
import { formatMovieMeta, formatRelativeDate } from "../utils/watchlistFormat";

type WatchlistListProps = {
  items: WatchedMovieRecord[];
  onRemove: (movieId: number) => void;
};

const WatchlistList = ({ items, onRemove }: WatchlistListProps) => (
  <Stack spacing={1}>
    {items.map((item) => {
      const movie = item.movie;
      const movieId = item.movieId ?? movie?.id;

      return (
        <Box
          key={movieId}
          sx={{
            alignItems: "center",
            backgroundColor: "background.paper",
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 1,
            display: "grid",
            gap: 1.5,
            gridTemplateColumns: {
              xs: "52px minmax(0, 1fr) auto",
              sm: "60px minmax(0, 1fr) 92px 120px auto",
            },
            p: 1,
          }}
        >
          <PosterImage
            imageUrlToken={movie?.imageUrlToken}
            size={MinioImageSize.Small}
            sx={{
              aspectRatio: "2 / 3",
              borderRadius: 0.75,
              height: "auto",
              objectFit: "cover",
              width: "100%",
            }}
          />
          <Box sx={{ minWidth: 0 }}>
            <Typography
              component={Link}
              to={`/movie?id=${movieId}`}
              sx={{
                color: "text.primary",
                display: "block",
                fontWeight: 600,
                overflow: "hidden",
                textDecoration: "none",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {movie?.primaryTitle ?? "Unknown title"}
            </Typography>
            <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
              {movie ? formatMovieMeta(movie) : ""}
            </Typography>
          </Box>
          <Typography
            sx={{
              color: "text.secondary",
              display: { xs: "none", sm: "block" },
              fontSize: 12,
            }}
          >
            {movie?.imdbRating !== undefined && (
              <>
                <StarIcon sx={{ color: movieColors.gold, fontSize: 14 }} />{" "}
                {movie.imdbRating}
              </>
            )}
          </Typography>
          <Typography
            sx={{
              color: "text.secondary",
              display: { xs: "none", sm: "block" },
              fontSize: 12,
            }}
          >
            {formatRelativeDate(item.addedAt)}
          </Typography>
          {movieId !== undefined && (
            <IconButton
              aria-label="Remove from watchlist"
              onClick={() => onRemove(movieId)}
              size="small"
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
      );
    })}
  </Stack>
);

export default WatchlistList;
