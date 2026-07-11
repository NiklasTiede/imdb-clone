import BookmarkIcon from "@mui/icons-material/Bookmark";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import {
  formatRatingCount,
  getMoviePosterToken,
  IMDB_GOLD,
  type Movie,
} from "../../catalog";
import { ObjectStorageImageSize, PosterImage } from "../../../shared/media";
import { movieColors } from "../../../theme";

type HeroProps = {
  movie: Movie | null;
  error?: boolean;
  loading?: boolean;
  isBookmarked?: boolean;
  isBookmarkLoading?: boolean;
  onToggleBookmark: () => void;
  onViewMovie: () => void;
};

const FEATURED_BADGE_BG = "rgba(245,197,24,0.14)";
const HERO_BACKGROUND = movieColors.surface;

const Hero = ({
  movie,
  error = false,
  loading = false,
  isBookmarked = false,
  isBookmarkLoading = false,
  onToggleBookmark,
  onViewMovie,
}: HeroProps) => {
  if (loading) {
    return <HeroSkeleton />;
  }

  if (error) {
    return <HeroFallback />;
  }

  if (!movie) {
    return null;
  }

  return (
    <Box
      component="section"
      sx={{
        backgroundColor: HERO_BACKGROUND,
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 2,
        mb: 5,
        minHeight: { xs: 280, md: 340 },
        overflow: "hidden",
        position: "relative",
      }}
    >
      <Box
        aria-hidden
        sx={{
          background: `linear-gradient(135deg, rgba(245,197,24,0.11) 0%, rgba(77,171,247,0.07) 44%, rgba(13,27,42,0) 74%), linear-gradient(90deg, ${movieColors.surface} 0%, rgba(13,27,42,0.86) 58%, rgba(30,42,58,0.64) 100%)`,
          inset: 0,
          position: "absolute",
        }}
      />
      <Stack
        direction={{ xs: "column", md: "row" }}
        spacing={{ xs: 2, md: 4 }}
        sx={{
          alignItems: "center",
          height: "100%",
          p: { xs: 3, md: 4 },
          position: "relative",
        }}
      >
        <Box
          sx={{
            flexShrink: 0,
            width: { xs: 110, md: 160 },
          }}
        >
          <PosterImage
            posterImageToken={getMoviePosterToken(movie)}
            size={ObjectStorageImageSize.Large}
            sx={{
              aspectRatio: "2 / 3",
              backgroundColor: movieColors.surfaceInset,
              border: "1px solid rgba(255,255,255,0.1)",
              borderRadius: 1,
              height: "auto",
              width: "100%",
            }}
          />
        </Box>

        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Chip
            label="FEATURED PICK"
            size="small"
            sx={{
              backgroundColor: FEATURED_BADGE_BG,
              color: "primary.main",
              fontSize: 10,
              fontWeight: 500,
              height: 22,
              letterSpacing: 0,
              mb: 1.5,
            }}
          />
          <Typography
            component="h1"
            variant="h3"
            sx={{
              fontSize: { xs: 24, md: 32 },
              fontWeight: 500,
              lineHeight: 1.15,
              mb: 1,
            }}
          >
            {movie.primaryTitle ?? "Featured movie"}
          </Typography>

          <HeroMeta movie={movie} />

          {movie.description && (
            <Typography
              sx={{
                color: "text.secondary",
                display: { xs: "-webkit-box", md: "block" },
                fontSize: 14,
                lineHeight: 1.5,
                maxWidth: 560,
                mb: 2.5,
                overflow: "hidden",
                WebkitBoxOrient: "vertical",
                WebkitLineClamp: 2,
              }}
            >
              {movie.description}
            </Typography>
          )}

          <Stack
            direction="row"
            spacing={1.5}
            useFlexGap
            sx={{ flexWrap: "wrap" }}
          >
            <Button
              variant="contained"
              startIcon={<PlayArrowIcon sx={{ fontSize: 18 }} />}
              onClick={onViewMovie}
              sx={{ textTransform: "none" }}
            >
              View movie
            </Button>
            <Button
              variant="outlined"
              startIcon={
                isBookmarked ? (
                  <BookmarkIcon sx={{ fontSize: 18 }} />
                ) : (
                  <BookmarkBorderIcon sx={{ fontSize: 18 }} />
                )
              }
              onClick={onToggleBookmark}
              disabled={isBookmarkLoading}
              sx={{
                borderColor: "rgba(255,255,255,0.25)",
                color: "text.primary",
                textTransform: "none",
                ...(isBookmarked && {
                  backgroundColor: "success.main",
                  borderColor: "success.main",
                  "&:hover": {
                    backgroundColor: "success.dark",
                    borderColor: "success.dark",
                  },
                }),
              }}
            >
              {isBookmarked ? "In watchlist" : "Add to watchlist"}
            </Button>
          </Stack>
        </Box>
      </Stack>
    </Box>
  );
};

const HeroMeta = ({ movie }: { movie: Movie }) => {
  const metaItems = [
    movie.startYear ? String(movie.startYear) : null,
    movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : null,
  ].filter((item): item is string => Boolean(item));

  return (
    <Stack
      direction="row"
      spacing={1.5}
      useFlexGap
      sx={{
        alignItems: "center",
        color: "text.secondary",
        flexWrap: "wrap",
        fontSize: 13,
        mb: 1.5,
      }}
    >
      {metaItems.map((item, index) => (
        <Stack
          key={item}
          direction="row"
          spacing={1.5}
          sx={{ alignItems: "center" }}
        >
          {index > 0 && (
            <Box component="span" sx={{ color: "text.disabled" }}>
              ·
            </Box>
          )}
          <Box component="span">{item}</Box>
        </Stack>
      ))}
      {movie.imdbRating !== undefined && (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
          {metaItems.length > 0 && (
            <Box component="span" sx={{ color: "text.disabled", mr: 1 }}>
              ·
            </Box>
          )}
          <StarIcon sx={{ color: IMDB_GOLD, fontSize: 16 }} />
          <Box component="span">{movie.imdbRating.toFixed(1)}</Box>
          <Box component="span" sx={{ color: "text.disabled", fontSize: 12 }}>
            ({formatRatingCount(movie.imdbRatingCount)})
          </Box>
        </Stack>
      )}
    </Stack>
  );
};

const HeroSkeleton = () => (
  <Box
    component="section"
    sx={{
      borderRadius: 2,
      mb: 5,
      minHeight: { xs: 280, md: 340 },
      overflow: "hidden",
      p: { xs: 3, md: 4 },
    }}
  >
    <Stack direction={{ xs: "column", md: "row" }} spacing={{ xs: 2, md: 4 }}>
      <Skeleton
        variant="rectangular"
        sx={{
          aspectRatio: "2 / 3",
          borderRadius: 1,
          width: { xs: 110, md: 160 },
        }}
      />
      <Box sx={{ flex: 1 }}>
        <Skeleton variant="rounded" width={120} height={22} sx={{ mb: 1.5 }} />
        <Skeleton variant="text" width="55%" height={44} />
        <Skeleton variant="text" width="35%" />
        <Skeleton variant="text" width="80%" />
        <Skeleton variant="text" width="72%" />
      </Box>
    </Stack>
  </Box>
);

const HeroFallback = () => (
  <Box
    component="section"
    sx={{
      backgroundColor: HERO_BACKGROUND,
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 2,
      mb: 5,
      overflow: "hidden",
      p: { xs: 3, md: 4 },
    }}
  >
    <Typography component="h1" sx={{ fontSize: 24, fontWeight: 600, mb: 1 }}>
      Featured movie unavailable
    </Typography>
    <Typography sx={{ color: "text.secondary", fontSize: 14 }}>
      The homepage is still ready to explore below.
    </Typography>
  </Box>
);

export default Hero;
