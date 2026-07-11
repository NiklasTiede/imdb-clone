import { CardMedia } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { useState } from "react";
import placeholderSearch from "../../assets/img/placeholder_search.png";
import {
  getMoviePosterFallbackImageUrl,
  getMoviePosterImageUrl,
  type MovieImageSize,
} from "./imageUrls";

type PosterImageProps = {
  alt?: string;
  posterImageToken?: string;
  size: MovieImageSize;
  sx?: SxProps<Theme>;
};

const PosterImage = ({
  alt = "movie poster",
  posterImageToken,
  size,
  sx,
}: PosterImageProps) => {
  const [failureState, setFailureState] = useState<{
    attempts: number;
    token?: string;
  }>({ attempts: 0 });
  const failedAttempts =
    failureState.token === posterImageToken ? failureState.attempts : 0;
  const src =
    !posterImageToken || failedAttempts >= 2
      ? placeholderSearch
      : failedAttempts === 1
        ? getMoviePosterFallbackImageUrl(posterImageToken, size)
        : getMoviePosterImageUrl(posterImageToken, size);

  return (
    <CardMedia
      component="img"
      alt={alt}
      sx={sx}
      src={src}
      onError={() => {
        if (posterImageToken && failedAttempts < 2) {
          setFailureState({
            attempts: failedAttempts + 1,
            token: posterImageToken,
          });
        }
      }}
    />
  );
};

export default PosterImage;
