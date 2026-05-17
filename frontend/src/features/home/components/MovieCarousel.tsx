import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useCallback, useEffect, useRef, useState } from "react";
import { movieColors } from "../../../theme";
import { PosterMovieCard, type Movie } from "../../catalog";
import {
  movieCarouselCardWidthSx,
  movieCarouselScrollSx,
} from "./MovieCarousel.styles";

type MovieCarouselProps = {
  title: string;
  subtitle?: string;
  movies: Movie[];
  onViewAll?: () => void;
  loading?: boolean;
};

const MovieCarousel = ({
  title,
  subtitle,
  movies,
  onViewAll,
  loading = false,
}: MovieCarouselProps) => {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);

  const updateScrollState = useCallback(() => {
    const element = scrollRef.current;
    if (!element) {
      return;
    }

    setCanScrollLeft(element.scrollLeft > 4);
    setCanScrollRight(
      element.scrollLeft + element.clientWidth < element.scrollWidth - 4,
    );
  }, []);

  useEffect(() => {
    updateScrollState();
  }, [loading, movies.length, updateScrollState]);

  const scroll = (direction: "left" | "right") => {
    const element = scrollRef.current;
    if (!element) {
      return;
    }

    element.scrollBy({
      behavior: "smooth",
      left:
        direction === "left"
          ? -(element.clientWidth * 0.8)
          : element.clientWidth * 0.8,
    });
  };

  const handleScroll = () => {
    updateScrollState();
  };

  return (
    <Box component="section" sx={{ mb: 5 }}>
      <Stack
        direction="row"
        sx={{
          alignItems: "center",
          gap: 2,
          justifyContent: "space-between",
          mb: 2,
          px: { xs: 2, md: 0 },
        }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography component="h2" variant="h5" sx={{ fontWeight: 500 }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography sx={{ color: "text.disabled", fontSize: 12, mt: 0.25 }}>
              {subtitle}
            </Typography>
          )}
        </Box>

        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          {onViewAll && (
            <Button
              size="small"
              endIcon={<ArrowForwardIcon sx={{ fontSize: 14 }} />}
              onClick={onViewAll}
              sx={{
                color: "text.secondary",
                fontSize: 13,
                textTransform: "none",
                whiteSpace: "nowrap",
              }}
            >
              View all
            </Button>
          )}
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ display: { xs: "none", md: "flex" } }}
          >
            <IconButton
              aria-label={`Scroll ${title} left`}
              size="small"
              onClick={() => scroll("left")}
              disabled={!canScrollLeft}
              sx={{
                backgroundColor: "background.paper",
                border: "1px solid",
                borderColor: "divider",
                "&:hover": { backgroundColor: movieColors.surfaceElevated },
                "&.Mui-disabled": { opacity: 0.3 },
              }}
            >
              <ChevronLeftIcon sx={{ fontSize: 18 }} />
            </IconButton>
            <IconButton
              aria-label={`Scroll ${title} right`}
              size="small"
              onClick={() => scroll("right")}
              disabled={!canScrollRight}
              sx={{
                backgroundColor: "background.paper",
                border: "1px solid",
                borderColor: "divider",
                "&:hover": { backgroundColor: movieColors.surfaceElevated },
                "&.Mui-disabled": { opacity: 0.3 },
              }}
            >
              <ChevronRightIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Stack>
        </Stack>
      </Stack>

      <Box
        ref={scrollRef}
        onScroll={handleScroll}
        sx={movieCarouselScrollSx}
      >
        {loading
          ? Array.from({ length: 6 }).map((_, index) => (
              <Box
                key={index}
                data-testid="movie-carousel-skeleton"
                sx={{
                  flex: "0 0 auto",
                  scrollSnapAlign: "start",
                  width: movieCarouselCardWidthSx,
                }}
              >
                <Skeleton
                  variant="rectangular"
                  sx={{ aspectRatio: "2 / 3", borderRadius: 1 }}
                />
                <Skeleton variant="text" sx={{ mt: 1 }} />
                <Skeleton variant="text" width="60%" />
              </Box>
            ))
          : movies.map((movie) => (
              <Box
                key={movie.id}
                sx={{
                  flex: "0 0 auto",
                  scrollSnapAlign: "start",
                  width: movieCarouselCardWidthSx,
                }}
              >
                <PosterMovieCard movie={movie} />
              </Box>
            ))}
      </Box>
    </Box>
  );
};

export default MovieCarousel;
