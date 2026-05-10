import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardActionArea from "@mui/material/CardActionArea";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import { Link } from "react-router";
import { MovieRecord } from "../../../client/movies/generator-output";
import { IMDB_GOLD } from "../../catalog/components/RatingPill";
import { MinioImageSize, PosterImage } from "../../../shared/media";

type SearchMovieCardProps = {
  movie: MovieRecord;
  onToggleBookmark?: (movieId: number) => void;
};

const SearchMovieCard = ({ movie, onToggleBookmark }: SearchMovieCardProps) => {
  const detailUrl = `/movie?id=${movie.id}`;

  return (
    <Card
      sx={{
        backgroundColor: "transparent",
        boxShadow: "none",
        overflow: "visible",
      }}
    >
      <CardActionArea
        component={Link}
        to={detailUrl}
        sx={{
          color: "text.primary",
          display: "block",
          textAlign: "left",
          textDecoration: "none",
          "&:hover .search-card-poster": {
            transform: "translateY(-2px)",
          },
        }}
      >
        <Box
          className="search-card-poster"
          sx={{
            aspectRatio: "2 / 3",
            backgroundColor: "background.paper",
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 1,
            overflow: "hidden",
            position: "relative",
            transition: "transform 150ms ease",
          }}
        >
          <PosterImage
            imageUrlToken={movie.imageUrlToken}
            size={MinioImageSize.Large}
            sx={{
              height: "100%",
              objectFit: "cover",
              width: "100%",
            }}
          />

          {onToggleBookmark && movie.id !== undefined && (
            <IconButton
              aria-label="Add to watchlist"
              size="small"
              onClick={(event) => {
                event.preventDefault();
                event.stopPropagation();
                onToggleBookmark(movie.id as number);
              }}
              sx={{
                backgroundColor: "rgba(0,0,0,0.65)",
                color: "common.white",
                position: "absolute",
                right: 0.75,
                top: 0.75,
                "&:hover": {
                  backgroundColor: "rgba(0,0,0,0.78)",
                },
              }}
            >
              <BookmarkBorderIcon fontSize="small" />
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
          {movie.primaryTitle}
        </Typography>
        <Typography sx={{ color: "text.secondary", fontSize: 12, mt: 0.25 }}>
          {formatMeta(movie)}
        </Typography>
      </CardActionArea>
    </Card>
  );
};

const formatMeta = (movie: MovieRecord): string =>
  [movie.startYear, movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : null]
    .filter(Boolean)
    .join(" · ");

export default SearchMovieCard;
