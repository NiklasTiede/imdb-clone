import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import CheckIcon from "@mui/icons-material/Check";
import IosShareIcon from "@mui/icons-material/IosShare";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import {
  BackdropImage,
  ObjectStorageImageSize,
  PosterImage,
} from "../../../shared/media";
import { movieColors } from "../../../theme";
import type { Movie } from "../model/movie";
import {
  getMovieGenreLabels,
  getMovieMetaItems,
  getOriginalTitle,
} from "../model/moviePresentation";
import { COMMUNITY_BLUE, IMDB_GOLD, RatingPill } from "./RatingPill";

type MovieHeroProps = {
  isBookmarked: boolean;
  isBookmarkLoading?: boolean;
  isRatingLoading?: boolean;
  isShareDisabled?: boolean;
  movie: Movie;
  onOpenRating: () => void;
  onShare: () => void;
  onToggleBookmark: () => void;
  userRating: number | null;
};

export const MovieHero = ({
  isBookmarked,
  isBookmarkLoading = false,
  isRatingLoading = false,
  isShareDisabled = false,
  movie,
  onOpenRating,
  onShare,
  onToggleBookmark,
  userRating,
}: MovieHeroProps) => {
  const genreLabels = getMovieGenreLabels(movie);
  const metaItems = getMovieMetaItems(movie);
  const originalTitle = getOriginalTitle(movie);
  const title = movie.primaryTitle?.trim() || "Untitled movie";
  return (
    <Box
      component="section"
      data-testid="movie-detail-hero"
      aria-labelledby="movie-detail-title"
      sx={{ color: "common.white", pb: { xs: 1, md: 2 } }}
    >
      <Box
        sx={{
          borderRadius: 1,
          overflow: "hidden",
          position: "relative",
        }}
      >
        <BackdropImage
          backdropImageToken={movie.backdropImageToken}
          sx={{
            height: { xs: 220, sm: 300, md: 430 },
            maskImage:
              "linear-gradient(to bottom, black 0%, black 48%, transparent 100%)",
            WebkitMaskImage:
              "linear-gradient(to bottom, black 0%, black 48%, transparent 100%)",
          }}
        />
        <Box
          aria-hidden
          sx={{
            background: {
              xs: `linear-gradient(180deg, rgba(7,11,18,0.08) 0%, rgba(7,11,18,0.5) 46%, ${movieColors.backdrop} 100%)`,
              md: `linear-gradient(90deg, rgba(7,11,18,0.22) 0%, rgba(7,11,18,0.58) 52%, rgba(7,11,18,0.38) 100%), linear-gradient(180deg, rgba(7,11,18,0.04) 18%, rgba(7,11,18,0.66) 58%, ${movieColors.backdrop} 100%)`,
            },
            inset: 0,
            position: "absolute",
          }}
        />
      </Box>

      <Box
        sx={{
          display: "grid",
          gap: { xs: 2, sm: 2.5, md: 2 },
          gridTemplateAreas: {
            xs: '"poster identity" "actions actions" "ratings ratings"',
            md: '"poster content"',
          },
          gridTemplateColumns: {
            xs: "104px minmax(0, 1fr)",
            sm: "140px minmax(0, 1fr)",
            md: "220px minmax(0, 1fr)",
          },
          gridTemplateRows: { md: "330px" },
          height: { md: 330 },
          mx: { xs: 1.5, sm: 3, md: 4 },
          mt: { xs: -7, sm: -11, md: -24 },
          position: "relative",
          zIndex: 1,
        }}
      >
        <Box sx={{ gridArea: "poster", minWidth: 0 }}>
          <PosterImage
            alt={`${title} poster`}
            posterImageToken={movie.posterImageToken}
            size={ObjectStorageImageSize.Large}
            sx={{
              aspectRatio: "2 / 3",
              backgroundColor: movieColors.surfaceInset,
              border: "1px solid rgba(255,255,255,0.12)",
              borderRadius: 1,
              boxShadow: "0 18px 42px rgba(0,0,0,0.42)",
              height: "auto",
              width: "100%",
            }}
          />
        </Box>

        <Box
          sx={{
            display: { xs: "contents", md: "flex" },
            flexDirection: { md: "column" },
            gap: { md: 1.5 },
            gridArea: { md: "content" },
            justifyContent: { md: "flex-end" },
            minWidth: 0,
          }}
        >
          <Stack
            data-testid="movie-detail-identity"
            spacing={{ xs: 1, sm: 1.25 }}
            sx={{
              alignSelf: { xs: "end", md: "stretch" },
              gridArea: "identity",
              minWidth: 0,
            }}
          >
            <Box>
              <Typography
                id="movie-detail-title"
                component="h1"
                sx={{
                  fontSize: { xs: 24, sm: 30, md: 38 },
                  fontWeight: 600,
                  lineHeight: 1.12,
                  overflowWrap: "anywhere",
                  textShadow: "0 2px 16px rgba(0,0,0,0.72)",
                }}
              >
                {title}
              </Typography>
              {originalTitle && (
                <Typography
                  sx={{
                    color: "rgba(255,255,255,0.65)",
                    fontSize: { xs: 11, sm: 12 },
                    mt: 0.5,
                  }}
                >
                  Original title: {originalTitle}
                </Typography>
              )}
            </Box>

            <MetaRow items={metaItems} />

            {genreLabels.length > 0 && (
              <Stack
                direction="row"
                spacing={0.75}
                useFlexGap
                sx={{ flexWrap: "wrap" }}
              >
                {genreLabels.map((genre) => (
                  <Chip
                    key={genre}
                    label={genre}
                    size="small"
                    sx={{
                      backgroundColor: "rgba(23,33,50,0.88)",
                      color: "rgba(255,255,255,0.88)",
                      fontSize: 11,
                      height: 24,
                    }}
                  />
                ))}
              </Stack>
            )}
          </Stack>

          <Stack
            data-testid="movie-detail-actions"
            direction="row"
            spacing={1}
            useFlexGap
            sx={{
              alignItems: "center",
              flexWrap: "wrap",
              gridArea: "actions",
            }}
          >
            <Button
              variant="contained"
              startIcon={isBookmarked ? <CheckIcon /> : <BookmarkBorderIcon />}
              onClick={onToggleBookmark}
              disabled={isBookmarkLoading}
              sx={{
                color: isBookmarked ? "common.white" : movieColors.brandInk,
                fontWeight: 700,
                height: 40,
                textTransform: "none",
                ...(isBookmarked && {
                  backgroundColor: "success.main",
                  "&:hover": { backgroundColor: "success.dark" },
                }),
              }}
            >
              {isBookmarked ? "In watchlist" : "Add to watchlist"}
            </Button>
            <Button
              variant="outlined"
              startIcon={<StarBorderIcon />}
              onClick={onOpenRating}
              disabled={isRatingLoading}
              sx={{
                borderColor: "rgba(255,255,255,0.24)",
                color: "common.white",
                height: 40,
                textTransform: "none",
                "&:hover": {
                  backgroundColor: "rgba(255,255,255,0.06)",
                  borderColor: "rgba(255,255,255,0.44)",
                },
              }}
            >
              {userRating === null
                ? "Rate movie"
                : `Your rating: ${userRating}`}
            </Button>
            <Tooltip title="Share movie">
              <span>
                <IconButton
                  aria-label="Share movie"
                  onClick={onShare}
                  disabled={isShareDisabled}
                  sx={{
                    border: "1px solid rgba(255,255,255,0.24)",
                    borderRadius: 1,
                    color: "common.white",
                    height: 40,
                    width: 40,
                  }}
                >
                  <IosShareIcon />
                </IconButton>
              </span>
            </Tooltip>
          </Stack>

          <Box
            data-testid="movie-detail-ratings"
            sx={{
              display: "grid",
              gap: 1,
              gridArea: "ratings",
              gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
              maxWidth: { md: 560 },
              width: "100%",
            }}
          >
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
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

const MetaRow = ({ items }: { items: string[] }) => {
  if (items.length === 0) {
    return null;
  }

  return (
    <Stack
      direction="row"
      spacing={1}
      useFlexGap
      sx={{
        alignItems: "center",
        color: "rgba(255,255,255,0.76)",
        flexWrap: "wrap",
        fontSize: 13,
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
            <Box component="span" sx={{ color: "rgba(255,255,255,0.35)" }}>
              ·
            </Box>
          )}
          <Box component="span">{item}</Box>
        </Stack>
      ))}
    </Stack>
  );
};

export default MovieHero;
