import Box from "@mui/material/Box";
import type { SxProps, Theme } from "@mui/material/styles";
import { useState } from "react";
import { getMovieBackdropImageUrl, MovieBackdropImageSize } from "./imageUrls";

type BackdropImageProps = {
  backdropImageToken?: string | null | undefined;
  sx?: SxProps<Theme>;
};

const BackdropImage = ({ backdropImageToken, sx }: BackdropImageProps) => {
  const [failedToken, setFailedToken] = useState<string | null>(null);
  const hasImage = Boolean(
    backdropImageToken && failedToken !== backdropImageToken,
  );

  return (
    <Box
      data-has-image={hasImage ? "true" : "false"}
      data-testid="movie-backdrop"
      sx={{
        backgroundColor: "surface.inset",
        backgroundImage: (theme) =>
          `linear-gradient(135deg, ${theme.alpha(theme.palette.accent.main, 0.09)}, ${theme.alpha(theme.palette.data.comparison, 0.05)} 42%, ${theme.palette.surface.page} 82%)`,
        overflow: "hidden",
        position: "relative",
        ...sx,
      }}
    >
      {hasImage && backdropImageToken && (
        <Box
          component="img"
          alt=""
          aria-hidden="true"
          src={getMovieBackdropImageUrl(
            backdropImageToken,
            MovieBackdropImageSize.Large,
          )}
          onError={() => setFailedToken(backdropImageToken)}
          sx={{
            height: "100%",
            inset: 0,
            objectFit: "cover",
            objectPosition: "center 28%",
            position: "absolute",
            width: "100%",
          }}
        />
      )}
    </Box>
  );
};

export default BackdropImage;
