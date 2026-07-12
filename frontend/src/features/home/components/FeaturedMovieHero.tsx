import BookmarkIcon from "@mui/icons-material/Bookmark";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import IconButton from "@mui/material/IconButton";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useEffect, useRef } from "react";
import {
  formatRatingCount,
  IMDB_GOLD,
  type Movie,
} from "../../catalog";
import { BackdropImage } from "../../../shared/media";
import { movieColors } from "../../../theme";

type FeaturedMovieHeroProps = {
  movies: Movie[];
  error?: boolean;
  loading?: boolean;
  bookmarkedMovieIds?: Set<number>;
  isBookmarkLoading?: boolean;
  onImpression?: () => void;
  onToggleBookmark: (movie: Movie) => void;
  onViewMovie: (movie: Movie, position: number) => void;
};

const emptyMovieIds = new Set<number>();

const FeaturedMovieHero = ({
  movies,
  error = false,
  loading = false,
  bookmarkedMovieIds = emptyMovieIds,
  isBookmarkLoading = false,
  onImpression,
  onToggleBookmark,
  onViewMovie,
}: FeaturedMovieHeroProps) => {
  const sectionRef = useRef<HTMLElement | null>(null);
  const hasReportedImpression = useRef(false);

  useEffect(() => {
    const element = sectionRef.current;
    if (!element || !onImpression || loading || movies.length === 0 || hasReportedImpression.current) {
      return;
    }
    const reportOnce = () => {
      if (!hasReportedImpression.current) {
        hasReportedImpression.current = true;
        onImpression();
      }
    };
    if (typeof IntersectionObserver === "undefined") {
      reportOnce();
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          reportOnce();
          observer.disconnect();
        }
      },
      { threshold: 0.35 },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [loading, movies.length, onImpression]);

  if (loading) {
    return <FeaturedMovieSkeleton />;
  }
  if (error) {
    return <FeaturedMovieFallback />;
  }
  if (movies.length === 0) {
    return null;
  }

  return (
    <Box component="section" ref={sectionRef} sx={{ mb: 5 }}>
      <Stack
        direction={{ xs: "column", sm: "row" }}
        sx={{
          alignItems: { xs: "flex-start", sm: "flex-end" },
          gap: 1,
          justifyContent: "space-between",
          mb: 2,
        }}
      >
        <Box>
          <Typography
            component="p"
            sx={{
              color: "primary.main",
              fontSize: 10,
              fontWeight: 800,
              letterSpacing: "0.16em",
              mb: 0.5,
              textTransform: "uppercase",
            }}
          >
            Curated daily
          </Typography>
          <Typography component="h1" variant="h4" sx={{ fontWeight: 700 }}>
            Featured today
          </Typography>
        </Box>
        <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
          <Box
            component="span"
            sx={{ display: { xs: "inline", sm: "none" } }}
          >
            One standout, refreshed with every new mix
          </Box>
          <Box
            component="span"
            sx={{ display: { xs: "none", sm: "inline" } }}
          >
            Three standouts, refreshed with every new mix
          </Box>
        </Typography>
      </Stack>

      <Box
        data-testid="featured-movie-grid"
        sx={{
          display: "grid",
          gap: { xs: 1.25, md: 1.5 },
          gridAutoRows: { md: "minmax(0, 1fr)" },
          gridTemplateColumns: {
            xs: "minmax(0, 1fr)",
            sm: "repeat(2, minmax(0, 1fr))",
            md: "minmax(0, 1.85fr) minmax(280px, 0.9fr)",
          },
          minHeight: { md: 440 },
        }}
      >
        {movies.slice(0, 3).map((movie, position) => (
          <FeaturedMovieCard
            key={movie.id ?? `${movie.primaryTitle}-${position}`}
            movie={movie}
            position={position}
            primary={position === 0}
            bookmarked={Boolean(movie.id && bookmarkedMovieIds.has(movie.id))}
            bookmarkLoading={isBookmarkLoading}
            onToggleBookmark={() => onToggleBookmark(movie)}
            onViewMovie={() => onViewMovie(movie, position)}
          />
        ))}
      </Box>
    </Box>
  );
};

const FeaturedMovieCard = ({
  movie,
  position,
  primary,
  bookmarked,
  bookmarkLoading,
  onToggleBookmark,
  onViewMovie,
}: {
  movie: Movie;
  position: number;
  primary: boolean;
  bookmarked: boolean;
  bookmarkLoading: boolean;
  onToggleBookmark: () => void;
  onViewMovie: () => void;
}) => {
  const meta = [
    movie.startYear ? String(movie.startYear) : null,
    movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : null,
  ].filter((item): item is string => Boolean(item));
  const genre = movie.movieGenre ? Array.from(movie.movieGenre)[0] : undefined;

  return (
    <Box
      component="article"
      data-testid="featured-movie-card"
      sx={{
        aspectRatio: { xs: primary ? "16 / 11" : "16 / 9", sm: "16 / 10", md: "auto" },
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 1,
        display: { xs: primary ? "block" : "none", sm: "block" },
        gridColumn: { xs: "auto", sm: primary ? "1 / -1" : "auto", md: "auto" },
        gridRow: { md: primary ? "1 / span 2" : "auto" },
        minHeight: { md: primary ? 440 : 212 },
        overflow: "hidden",
        position: "relative",
        transition: "border-color 180ms ease, transform 180ms ease",
        "&:hover": {
          borderColor: "rgba(245,197,24,0.5)",
          transform: "translateY(-2px)",
        },
        "&:hover [data-testid='movie-backdrop'] img": {
          transform: "scale(1.035)",
        },
      }}
    >
      <BackdropImage
        backdropImageToken={movie.backdropImageToken}
        sx={{
          height: "100%",
          inset: 0,
          position: "absolute",
          width: "100%",
          "& img": { transition: "transform 500ms ease" },
        }}
      />
      <Box
        aria-hidden
        sx={{
          background: primary
            ? `linear-gradient(90deg, rgba(7,11,18,0.94) 0%, rgba(7,11,18,0.7) 42%, rgba(7,11,18,0.12) 72%), linear-gradient(0deg, rgba(7,11,18,0.9) 0%, transparent 58%)`
            : "linear-gradient(0deg, rgba(7,11,18,0.96) 0%, rgba(7,11,18,0.2) 78%)",
          inset: 0,
          position: "absolute",
        }}
      />

      <Chip
        label={editorialLabel(movie, position)}
        size="small"
        sx={{
          backdropFilter: "blur(10px)",
          backgroundColor: primary ? "rgba(245,197,24,0.92)" : "rgba(7,11,18,0.72)",
          color: primary ? movieColors.brandInk : "common.white",
          fontSize: 9,
          fontWeight: 800,
          height: 23,
          left: { xs: 16, md: primary ? 24 : 16 },
          letterSpacing: "0.08em",
          position: "absolute",
          textTransform: "uppercase",
          top: { xs: 16, md: primary ? 24 : 16 },
        }}
      />

      <IconButton
        aria-label={bookmarked ? `Remove ${movie.primaryTitle} from watchlist` : `Add ${movie.primaryTitle} to watchlist`}
        disabled={bookmarkLoading}
        onClick={onToggleBookmark}
        sx={{
          backdropFilter: "blur(10px)",
          backgroundColor: bookmarked ? "primary.main" : "rgba(7,11,18,0.7)",
          color: bookmarked ? movieColors.brandInk : "common.white",
          height: 36,
          position: "absolute",
          right: { xs: 12, md: primary ? 20 : 12 },
          top: { xs: 12, md: primary ? 20 : 12 },
          width: 36,
          "&:hover": { backgroundColor: bookmarked ? "primary.dark" : "rgba(23,33,50,0.94)" },
        }}
      >
        {bookmarked ? <BookmarkIcon fontSize="small" /> : <BookmarkBorderIcon fontSize="small" />}
      </IconButton>

      <Box
        sx={{
          bottom: 0,
          left: 0,
          maxWidth: primary ? 610 : "100%",
          p: { xs: 2, md: primary ? 3 : 2 },
          position: "absolute",
          right: 0,
        }}
      >
        <Typography
          component={primary ? "h2" : "h3"}
          sx={{
            fontSize: primary ? { xs: 25, md: 38 } : { xs: 18, md: 20 },
            fontWeight: 750,
            lineHeight: 1.08,
            mb: 0.75,
            textShadow: "0 2px 18px rgba(0,0,0,0.6)",
          }}
        >
          {movie.primaryTitle ?? "Featured movie"}
        </Typography>

        <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: "center", color: "rgba(255,255,255,0.78)", flexWrap: "wrap", fontSize: 12 }}>
          {meta.map((item) => <Box component="span" key={item}>{item}</Box>)}
          {genre && <Box component="span">{humanize(genre)}</Box>}
          {movie.imdbRating !== undefined && (
            <Stack component="span" direction="row" spacing={0.4} sx={{ alignItems: "center" }}>
              <StarIcon sx={{ color: IMDB_GOLD, fontSize: 15 }} />
              <Box component="span" sx={{ color: "common.white", fontWeight: 700 }}>
                {movie.imdbRating.toFixed(1)}
              </Box>
              {primary && movie.imdbRatingCount !== undefined && (
                <Box component="span" sx={{ color: "rgba(255,255,255,0.58)" }}>
                  ({formatRatingCount(movie.imdbRatingCount)})
                </Box>
              )}
            </Stack>
          )}
        </Stack>

        {primary && movie.description && (
          <Typography
            sx={{
              color: "rgba(255,255,255,0.78)",
              display: { xs: "none", sm: "-webkit-box" },
              fontSize: 13,
              lineHeight: 1.5,
              maxWidth: 540,
              mt: 1.25,
              overflow: "hidden",
              WebkitBoxOrient: "vertical",
              WebkitLineClamp: 2,
            }}
          >
            {movie.description}
          </Typography>
        )}

        <Button
          aria-label={`View ${movie.primaryTitle ?? "featured movie"}`}
          color={primary ? "primary" : "inherit"}
          onClick={onViewMovie}
          size={primary ? "medium" : "small"}
          startIcon={<PlayArrowIcon />}
          variant={primary ? "contained" : "text"}
          sx={{
            color: primary ? movieColors.brandInk : "common.white",
            fontWeight: 700,
            ml: primary ? 0 : -1,
            mt: primary ? 2 : 0.75,
            textTransform: "none",
          }}
        >
          {primary ? "View movie" : "View details"}
        </Button>
      </Box>
    </Box>
  );
};

const editorialLabel = (movie: Movie, position: number) => {
  if ((movie.rating ?? 0) >= 8 && (movie.ratingCount ?? 0) >= 5) {
    return "Community favorite";
  }
  if ((movie.imdbRating ?? 0) >= 8.2 && (movie.imdbRatingCount ?? 0) >= 100_000) {
    return "Acclaimed standout";
  }
  if ((movie.startYear ?? 0) >= 2018) {
    return "Modern essential";
  }
  return position === 0 ? "Today's lead pick" : "Proven classic";
};

const humanize = (value: string) =>
  value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

const FeaturedMovieSkeleton = () => (
  <Box component="section" sx={{ mb: 5 }}>
    <Skeleton variant="text" width={140} height={34} sx={{ mb: 2 }} />
    <Box
      sx={{
        display: "grid",
        gap: 1.5,
        gridTemplateColumns: {
          xs: "1fr",
          sm: "repeat(2, minmax(0, 1fr))",
          md: "1.85fr 0.9fr",
        },
        minHeight: { md: 440 },
      }}
    >
      <Skeleton
        variant="rounded"
        sx={{
          gridColumn: { sm: "1 / -1", md: "auto" },
          gridRow: { md: "1 / span 2" },
          minHeight: { xs: 280, md: 440 },
        }}
      />
      <Skeleton
        variant="rounded"
        sx={{ display: { xs: "none", sm: "block" }, minHeight: 212 }}
      />
      <Skeleton
        variant="rounded"
        sx={{ display: { xs: "none", sm: "block" }, minHeight: 212 }}
      />
    </Box>
  </Box>
);

const FeaturedMovieFallback = () => (
  <Box
    component="section"
    sx={{
      backgroundColor: movieColors.surface,
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 1,
      mb: 5,
      p: { xs: 3, md: 4 },
    }}
  >
    <Typography component="h1" sx={{ fontSize: 24, fontWeight: 700, mb: 1 }}>
      Featured picks unavailable
    </Typography>
    <Typography sx={{ color: "text.secondary", fontSize: 14 }}>
      The homepage is still ready to explore below.
    </Typography>
  </Box>
);

export default FeaturedMovieHero;
