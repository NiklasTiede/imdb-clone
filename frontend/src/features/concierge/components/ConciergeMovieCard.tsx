import AccessTimeRoundedIcon from "@mui/icons-material/AccessTimeRounded";
import StarRoundedIcon from "@mui/icons-material/StarRounded";
import { Box, Card, Chip, Stack, Typography } from "@mui/material";
import { Link } from "react-router";
import { MoviePosterImageSize, PosterImage } from "../../../shared/media";
import { movieDetailPath } from "../../../shared/navigation/appRoutes";
import { accentTint } from "../../../theme";
import type { GroundedMovie } from "../model/concierge";

const ConciergeMovieCard = ({ movie }: { movie: GroundedMovie }) => (
  <Card
    data-testid="concierge-movie-card"
    variant="outlined"
    sx={{
      bgcolor: (theme) => theme.alpha(theme.palette.surface.raised, 0.9),
      borderColor: accentTint(0.18),
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
          color: "text.primary",
          display: "block",
          fontSize: 15,
          fontWeight: 700,
          lineHeight: 1.25,
          mb: 0.6,
          textDecoration: "none",
          "&:hover": { color: "accent.main" },
        }}
      >
        {movie.primaryTitle}
      </Typography>
      <Stack
        direction="row"
        spacing={1.1}
        useFlexGap
        sx={{ alignItems: "center", flexWrap: "wrap", mb: 1 }}
      >
        {movie.startYear != null && (
          <Typography sx={{ color: "text.secondary", fontSize: 11 }}>
            {movie.startYear}
          </Typography>
        )}
        {movie.runtimeMinutes != null && (
          <Stack
            direction="row"
            spacing={0.35}
            sx={{ alignItems: "center", color: "text.secondary" }}
          >
            <AccessTimeRoundedIcon sx={{ color: "inherit", fontSize: 13 }} />
            <Typography sx={{ color: "inherit", fontSize: 11 }}>
              {movie.runtimeMinutes} min
            </Typography>
          </Stack>
        )}
        {movie.imdbRating != null && (
          <Stack direction="row" spacing={0.25} sx={{ alignItems: "center" }}>
            <StarRoundedIcon sx={{ color: "star", fontSize: 15 }} />
            <Typography sx={{ color: "text.primary", fontSize: 11 }}>
              IMDb {movie.imdbRating.toFixed(1)}
            </Typography>
          </Stack>
        )}
      </Stack>
      {movie.userScore != null && (
        <Typography
          sx={{ color: "accent.core", fontSize: 12, fontWeight: 700, mb: 1 }}
        >
          Your rating: {movie.userScore}/10
        </Typography>
      )}
      {movie.explanation && (
        <Typography
          sx={{
            color: "text.secondary",
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
            color: "text.secondary",
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
                bgcolor: (theme) =>
                  theme.alpha(theme.palette.data.comparison, 0.1),
                color: "text.secondary",
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
