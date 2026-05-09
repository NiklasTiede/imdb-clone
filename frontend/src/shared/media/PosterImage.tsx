import { CardMedia } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import placeholderSearch from "../../assets/img/placeholder_search.png";
import { getMovieImageUrl, type MovieImageSize } from "./imageUrls";

type PosterImageProps = {
  imageUrlToken?: string;
  size: MovieImageSize;
  sx?: SxProps<Theme>;
};

const PosterImage = ({ imageUrlToken, size, sx }: PosterImageProps) => (
  <CardMedia
    component="img"
    alt="movie poster"
    sx={sx}
    src={
      imageUrlToken ? getMovieImageUrl(imageUrlToken, size) : placeholderSearch
    }
  />
);

export default PosterImage;
