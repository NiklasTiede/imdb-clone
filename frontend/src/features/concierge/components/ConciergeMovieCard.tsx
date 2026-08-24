import AccessTimeRoundedIcon from "@mui/icons-material/AccessTimeRounded";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { alpha } from "@mui/material/styles";
import { Box, Card, Chip, Stack, Typography } from "@mui/material";
import { Link } from "react-router";
import { MoviePosterImageSize, PosterImage } from "../../../shared/media";
import { movieDetailPath } from "../../../shared/navigation/appRoutes";
import { movieColors } from "../../../theme";
import type { GroundedMovie } from "../model/concierge";

const ConciergeMovieCard = ({ movie }: { movie: GroundedMovie }) => (
  <Card
    data-testid="concierge-movie-card"
    variant="outlined"
    sx={{
      bgcolor: alpha(movieColors.surfaceElevated, 0.9),
      borderColor: alpha(movieColors.brand, 0.18),
      borderRadius: 2,
      display: "grid",
      gridTemplateColumns: "82px minmax(0, 1fr)",
      minHeight: 124,
      overflow: "hidden",
    }}
  >
    <PosterImage
      alt={`${movie.primaryTitle} poster`}
      posterImageToken={movie.posterImageToken ?? undefined}
      size={MoviePosterImageSize.Small}
      sx={{ height: "100%", minHeight: 124, objectFit: "cover", width: 82 }}
    />
    <Box sx={{ minWidth: 0, p: 1.5 }}>
      <Typography
        component={Link}
        to={movieDetailPath(movie.movieId)}
        sx={{
          color: "common.white",
          display: "block",
          fontSize: 15,
          fontWeight: 700,
          lineHeight: 1.25,
          mb: 0.6,
          textDecoration: "none",
          "&:hover": { color: movieColors.brand },
        }}
      >
        {movie.primaryTitle}
      </Typography>
      <Stack
        direction="row"
        spacing={1.1}
        useFlexGap
        sx={{ alignItems: "center", mb: 1 }}
      >
        {movie.startYear != null && (
          <Typography sx={{ color: "rgba(255,255,255,0.78)", fontSize: 11 }}>
            {movie.startYear}
          </Typography>
        )}
        {movie.runtimeMinutes != null && (
          <Stack
            direction="row"
            spacing={0.35}
            sx={{ alignItems: "center", color: "rgba(255,255,255,0.78)" }}
          >
            <AccessTimeRoundedIcon sx={{ color: "inherit", fontSize: 13 }} />
            <Typography sx={{ color: "inherit", fontSize: 11 }}>
              {movie.runtimeMinutes} min
            </Typography>
          </Stack>
        )}
        {movie.imdbRating != null && (
          <Stack direction="row" spacing={0.25} sx={{ alignItems: "center" }}>
            <StarRoundedIcon sx={{ color: movieColors.rating, fontSize: 15 }} />
            <Typography sx={{ color: "rgba(255,255,255,0.92)", fontSize: 11 }}>
              {movie.imdbRating.toFixed(1)}
            </Typography>
          </Stack>
        )}
      </Stack>
      {movie.explanation && (
        <Typography
          sx={{
            color: "rgba(255,255,255,0.78)",
            fontSize: 11.5,
            lineHeight: 1.45,
          }}
        >
          {movie.explanation}
        </Typography>
      )}
      {!movie.explanation && movie.description && (
        <Typography
          sx={{
            color: "rgba(255,255,255,0.68)",
            display: "-webkit-box",
            fontSize: 11.5,
            lineHeight: 1.45,
            overflow: "hidden",
            WebkitBoxOrient: "vertical",
            WebkitLineClamp: 2,
          }}
        >
          {movie.description}
        </Typography>
      )}
      {movie.genres.length > 0 && (
        <Stack
          direction="row"
          spacing={0.5}
          useFlexGap
          sx={{ flexWrap: "wrap", mt: 1 }}
        >
          {movie.genres.slice(0, 3).map((genre) => (
            <Chip
              key={genre}
              label={genre.replaceAll("_", " ").toLowerCase()}
              size="small"
              sx={{
                bgcolor: alpha(movieColors.info, 0.1),
                color: "rgba(255,255,255,0.72)",
                fontSize: 9,
                height: 20,
                textTransform: "capitalize",
              }}
            />
          ))}
        </Stack>
      )}
    </Box>
  </Card>
);

export default ConciergeMovieCard;
