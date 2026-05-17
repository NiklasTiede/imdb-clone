import { CardMedia } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { useMemo, useState } from "react";
import placeholderSearch from "../../assets/img/placeholder_search.png";
import {
  getMoviePosterFallbackImageUrl,
  getMoviePosterImageUrl,
  type MovieImageSize,
} from "./imageUrls";

type PosterImageProps = {
  imageUrlToken?: string;
  size: MovieImageSize;
  sx?: SxProps<Theme>;
};

const PosterImage = ({ imageUrlToken, size, sx }: PosterImageProps) => {
  const [useFallback, setUseFallback] = useState(false);
  const src = useMemo(() => {
    if (!imageUrlToken) {
      return placeholderSearch;
    }

    return useFallback
      ? getMoviePosterFallbackImageUrl(imageUrlToken, size)
      : getMoviePosterImageUrl(imageUrlToken, size);
  }, [imageUrlToken, size, useFallback]);

  return (
    <CardMedia
      component="img"
      alt="movie poster"
      sx={sx}
      src={src}
      onError={() => setUseFallback(true)}
    />
  );
};

export default PosterImage;
