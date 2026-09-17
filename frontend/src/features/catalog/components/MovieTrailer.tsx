import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import type { SxProps, Theme } from "@mui/material/styles";
import { useState } from "react";
import { BackdropImage } from "../../../shared/media";
import { getYouTubeNoCookieEmbedUrl } from "../utils/youtubeTrailer";

type MovieTrailerProps = {
  backdropImageToken?: string | null | undefined;
  movieTitle: string;
  sx?: SxProps<Theme>;
  youtubeVideoKey?: string | null | undefined;
};

const MovieTrailer = ({
  backdropImageToken,
  movieTitle,
  sx,
  youtubeVideoKey,
}: MovieTrailerProps) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const embedUrl = getYouTubeNoCookieEmbedUrl(youtubeVideoKey);

  if (!embedUrl) {
    return null;
  }

  return (
    <Box
      component="section"
      aria-label="Movie trailer"
      data-testid="movie-trailer"
      sx={{
        aspectRatio: "16 / 9",
        backgroundColor: "surface.inset",
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 1,
        boxShadow: (theme) =>
          `0 18px 42px ${theme.alpha(theme.palette.scrim, 0.32)}`,
        minHeight: 0,
        overflow: "hidden",
        position: "relative",
        width: "100%",
        ...sx,
      }}
    >
      {isPlaying ? (
        <Box
          component="iframe"
          src={embedUrl}
          title={`${movieTitle} trailer`}
          allow="autoplay; encrypted-media; picture-in-picture"
          allowFullScreen
          loading="lazy"
          referrerPolicy="strict-origin-when-cross-origin"
          sx={{
            border: 0,
            height: "100%",
            inset: 0,
            position: "absolute",
            width: "100%",
          }}
        />
      ) : (
        <>
          <BackdropImage
            backdropImageToken={backdropImageToken}
            sx={{ height: "100%", inset: 0, position: "absolute" }}
          />
          <Box
            aria-hidden
            sx={{
              background: (theme) =>
                `linear-gradient(180deg, ${theme.alpha(theme.palette.scrim, 0.2)}, ${theme.alpha(theme.palette.scrim, 0.72)})`,
              inset: 0,
              position: "absolute",
            }}
          />
          <Button
            variant="contained"
            startIcon={<PlayArrowIcon />}
            onClick={() => setIsPlaying(true)}
            sx={{
              fontWeight: 700,
              left: "50%",
              position: "absolute",
              textTransform: "none",
              top: "50%",
              transform: "translate(-50%, -50%)",
              whiteSpace: "nowrap",
            }}
          >
            Play trailer
          </Button>
        </>
      )}
    </Box>
  );
};

export default MovieTrailer;
