import CloseIcon from "@mui/icons-material/CloseSharp";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Link } from "react-router";
import { MinioImageSize, PosterImage } from "../../../../shared/media";
import { movieColors } from "../../../../theme";
import { formatMovieMeta } from "../../watchlist";
import type { RatedMovie } from "../api/ratingQueries";

type RatingsListProps = {
  items: RatedMovie[];
  onRemove?: (movieId: number) => void;
};

const RatingsList = ({ items, onRemove }: RatingsListProps) => (
  <Stack aria-label="Rated movies" component="ul" role="list" spacing={1}>
    {items.map((item) => {
      const movie = item.movie;
      const movieId = movie.id;

      return (
        <Box
          component="li"
          key={movieId ?? movie.primaryTitle}
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
              sm: "60px minmax(0, 1fr) 78px 78px auto",
            },
            listStyle: "none",
            p: 1,
          }}
        >
          <PosterImage
            imageUrlToken={movie.imageUrlToken}
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
              component={movieId === undefined ? "span" : Link}
              to={movieId === undefined ? undefined : `/movie?id=${movieId}`}
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
              {movie.primaryTitle ?? "Unknown title"}
            </Typography>
            <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
              {formatMovieMeta(movie)}
            </Typography>
          </Box>
          <Typography
            sx={{
              alignItems: "center",
              color: movieColors.info,
              display: { xs: "none", sm: "inline-flex" },
              fontSize: 13,
              fontWeight: 700,
              gap: 0.25,
            }}
          >
            <StarIcon sx={{ fontSize: 15 }} /> {item.rating}/10
          </Typography>
          <Typography
            sx={{
              color: "text.secondary",
              display: { xs: "none", sm: "block" },
              fontSize: 12,
            }}
          >
            {movie.imdbRating !== undefined && (
              <>
                <StarIcon sx={{ color: movieColors.gold, fontSize: 14 }} />{" "}
                {movie.imdbRating}
              </>
            )}
          </Typography>
          {movieId !== undefined && onRemove && (
            <IconButton
              aria-label="Delete rating"
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

export default RatingsList;
