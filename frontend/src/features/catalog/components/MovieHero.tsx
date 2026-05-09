import { useState } from "react";
import { Box, Button, Chip, Rating, Stack, Typography } from "@mui/material";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import CheckIcon from "@mui/icons-material/Check";
import IosShareIcon from "@mui/icons-material/IosShare";
import StarIcon from "@mui/icons-material/Star";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import {
  MovieRecord,
  MovieRecordMovieTypeEnum,
} from "../../../client/movies/generator-output";
import { MinioImageSize, PosterImage } from "../../../shared/media";
import { COMMUNITY_BLUE, IMDB_GOLD, RatingPill } from "./RatingPill";

const humanizeEnum = (value: string): string =>
  value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

const HERO_BACKDROP = "#102236";

const SERIES_TYPES = new Set<string>([
  MovieRecordMovieTypeEnum.TvSeries,
  MovieRecordMovieTypeEnum.TvMiniSeries,
]);

type MovieHeroProps = {
  movie: MovieRecord;
  isBookmarked: boolean;
  onToggleBookmark: () => void;
  isBookmarkLoading?: boolean;
  userRating: number | null;
  onRate: (score: number) => void;
};

const formatYearRange = (movie: MovieRecord): string => {
  if (
    movie.movieType &&
    SERIES_TYPES.has(movie.movieType) &&
    movie.endYear &&
    movie.startYear
  ) {
    return `${movie.startYear} – ${movie.endYear}`;
  }
  return movie.startYear ? String(movie.startYear) : "";
};

export const MovieHero = ({
  movie,
  isBookmarked,
  onToggleBookmark,
  isBookmarkLoading = false,
  userRating,
  onRate,
}: MovieHeroProps) => {
  const showOriginalTitle =
    movie.originalTitle &&
    movie.primaryTitle &&
    movie.originalTitle !== movie.primaryTitle;

  const yearLabel = formatYearRange(movie);
  const typeLabel = movie.movieType
    ? humanizeEnum(String(movie.movieType))
    : "";
  const runtimeLabel =
    movie.runtimeMinutes != null ? `${movie.runtimeMinutes} min` : null;

  return (
    <Box
      sx={{
        position: "relative",
        px: { xs: 2, sm: 3 },
        py: { xs: 3, sm: 3.5 },
        backgroundColor: "#0d1b2a",
        color: "#fff",
      }}
    >
      <Box
        aria-hidden
        sx={{
          position: "absolute",
          inset: 0,
          height: { xs: 160, sm: 200 },
          backgroundColor: HERO_BACKDROP,
          zIndex: 0,
        }}
      />

      <Stack
        direction={{ xs: "column", md: "row" }}
        spacing={{ xs: 2, md: 2.5 }}
        sx={{ position: "relative", zIndex: 1 }}
      >
        <Box
          sx={{
            width: { xs: 130, md: 150 },
            flexShrink: 0,
            mx: { xs: "auto", md: 0 },
          }}
        >
          <PosterImage
            imageUrlToken={movie.imageUrlToken}
            size={MinioImageSize.Large}
            sx={{
              width: "100%",
              aspectRatio: "2 / 3",
              borderRadius: 1,
              border: "1px solid rgba(255,255,255,0.05)",
              backgroundColor: "#050a14",
            }}
          />
        </Box>

        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            component="h1"
            sx={{
              fontSize: { xs: 22, sm: 24 },
              fontWeight: 500,
              lineHeight: 1.2,
              mb: 0.25,
            }}
          >
            {movie.primaryTitle}
          </Typography>
          {showOriginalTitle && (
            <Typography
              sx={{
                color: "rgba(255,255,255,0.45)",
                fontSize: 12,
                fontStyle: "italic",
                mb: 1.5,
              }}
            >
              Original: {movie.originalTitle}
            </Typography>
          )}

          <MetaRow
            year={yearLabel}
            type={typeLabel}
            runtime={runtimeLabel}
            adult={movie.adult ?? false}
          />

          <Stack
            direction="row"
            spacing={0.75}
            useFlexGap
            sx={{ flexWrap: "wrap", mb: 1.75 }}
          >
            {movie.movieGenre &&
              Array.from(movie.movieGenre).map((genre) => (
                <Chip
                  key={String(genre)}
                  label={humanizeEnum(String(genre))}
                  size="small"
                  sx={{
                    backgroundColor: "#283548",
                    color: "rgba(255,255,255,0.85)",
                    fontSize: 12,
                    height: 24,
                  }}
                />
              ))}
          </Stack>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={1.25}>
            <RatingPill
              label="IMDb rating"
              score={movie.imdbRating}
              count={movie.imdbRatingCount}
              starColor={IMDB_GOLD}
            />
            <RatingPill
              label="Community"
              score={movie.rating}
              count={movie.ratingCount}
              starColor={COMMUNITY_BLUE}
            />
          </Stack>
        </Box>
      </Stack>

      <Stack
        direction="row"
        spacing={1.25}
        useFlexGap
        sx={{
          alignItems: "center",
          flexWrap: "wrap",
          mt: 2.5,
          position: "relative",
          zIndex: 1,
        }}
      >
        <Button
          variant="contained"
          startIcon={isBookmarked ? <CheckIcon /> : <BookmarkBorderIcon />}
          onClick={onToggleBookmark}
          disabled={isBookmarkLoading}
          sx={{
            textTransform: "none",
            fontWeight: 500,
            color: "#fff",
            backgroundColor: isBookmarked ? "#2e7d32" : "#1976d2",
            "&:hover": {
              backgroundColor: isBookmarked ? "#256528" : "#1565c0",
            },
          }}
        >
          {isBookmarked ? "In your watchlist" : "Add to watchlist"}
        </Button>

        <UserRatingStars value={userRating} onRate={onRate} />

        <Button
          variant="outlined"
          startIcon={<IosShareIcon />}
          sx={{
            textTransform: "none",
            color: "rgba(255,255,255,0.85)",
            borderColor: "rgba(255,255,255,0.2)",
            "&:hover": {
              borderColor: "rgba(255,255,255,0.42)",
              backgroundColor: "rgba(255,255,255,0.06)",
            },
          }}
        >
          Share
        </Button>
      </Stack>
    </Box>
  );
};

type MetaRowProps = {
  year: string;
  type: string;
  runtime: string | null;
  adult: boolean;
};

const MetaRow = ({ year, type, runtime, adult }: MetaRowProps) => {
  const items = [year, type, runtime, adult ? "18+" : null].filter(
    (item): item is string => Boolean(item),
  );
  return (
    <Stack
      direction="row"
      spacing={1}
      useFlexGap
      sx={{
        flexWrap: "wrap",
        color: "rgba(255,255,255,0.75)",
        fontSize: 13,
        mb: 1.5,
      }}
    >
      {items.map((item, index) => (
        <Stack
          key={`${item}-${index}`}
          direction="row"
          spacing={1}
          sx={{ alignItems: "center" }}
        >
          {index > 0 && (
            <Box component="span" sx={{ color: "rgba(255,255,255,0.3)" }}>
              ·
            </Box>
          )}
          <Box component="span">{item}</Box>
        </Stack>
      ))}
    </Stack>
  );
};

type UserRatingStarsProps = {
  value: number | null;
  onRate: (score: number) => void;
};

const UserRatingStars = ({ value, onRate }: UserRatingStarsProps) => {
  const [hover, setHover] = useState<number>(-1);

  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={{ xs: 0.5, sm: 0.75 }}
      data-testid="user-rating-stars"
      sx={{
        alignItems: { xs: "flex-start", sm: "center" },
        backgroundColor: "#1e2a3a",
        border: "1px solid rgba(255,255,255,0.06)",
        borderRadius: 1,
        px: 1.5,
        py: { xs: 0.75, sm: 0.5 },
        maxWidth: "100%",
      }}
    >
      <Typography
        sx={{
          color: "rgba(255,255,255,0.72)",
          fontSize: 12,
          whiteSpace: "nowrap",
        }}
      >
        Your rating
      </Typography>
      <Rating
        max={10}
        value={value}
        onChange={(event, newValue) => {
          const fallbackValue = Number(
            (event.target as HTMLInputElement).value,
          );
          const nextValue =
            typeof newValue === "number" && Number.isFinite(newValue)
              ? newValue
              : fallbackValue;

          if (Number.isFinite(nextValue)) {
            onRate(nextValue);
          }
        }}
        onChangeActive={(_event, newHover) => setHover(newHover)}
        icon={<StarIcon fontSize="inherit" />}
        emptyIcon={<StarBorderIcon fontSize="inherit" />}
        size="small"
        sx={{
          color: IMDB_GOLD,
          "& .MuiRating-iconEmpty": {
            color: "rgba(255,255,255,0.35)",
          },
        }}
      />
      {hover !== -1 && (
        <Typography sx={{ color: "rgba(255,255,255,0.72)", fontSize: 12 }}>
          {hover}/10
        </Typography>
      )}
    </Stack>
  );
};

export default MovieHero;
