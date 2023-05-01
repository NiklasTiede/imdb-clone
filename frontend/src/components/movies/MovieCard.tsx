import {
  Box,
  Card,
  CardMedia,
  Paper,
  Stack,
  styled,
  Typography,
  useTheme,
} from "@mui/material";
import { tokens } from "../../theme";
import {
  Movie,
  MovieMovieGenreEnum,
} from "../../client/movies/generator-output";
import React from "react";
import { Link } from "react-router-dom";
import { getMinioImageUrl, MinioImageSize } from "../../utils/imageUrlParser";

export const MovieLink = styled(Link)`
  text-decoration: none;
  &:hover {
    color: darkgray;
  }
`;

const Item = styled(Paper)(({ theme }) => ({
  backgroundColor: theme.palette.mode === "dark" ? "#1A2027" : "#fff",
  ...theme.typography.body2,
  padding: theme.spacing(0.5),
  textAlign: "center",
  fontSize: 10,
  color: theme.palette.text.secondary,
}));

export function snakeToPascalCase(str: string): string {
  const camelCase = str
    .toLowerCase()
    .replace(/_(\w)/g, (_, letter) => letter.toUpperCase());
  return camelCase.charAt(0).toUpperCase() + camelCase.slice(1);
}

const MovieCard = (movie: Movie) => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const imageUrl = movie?.imageUrlToken
    ? getMinioImageUrl(movie?.imageUrlToken, MinioImageSize.Small)
    : null;

  return (
    <div>
      <Card sx={{ display: "flex", width: 600, height: 100 }}>
        <CardMedia
          component="img"
          alt="movie poster"
          sx={{ width: 80, height: 100, padding: 1 }}
          src={
            movie.imageUrlToken
              ? imageUrl
              : require("../../assets/img/placeholder_search.png")
          }
        />
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between",
            width: 470,
          }}
        >
          <Typography sx={{ margin: 0.7, fontSize: 17 }}>
            <MovieLink
              to={`/movie?id=${movie.id}`}
              sx={{ color: colors.grey[100] }}
            >
              {movie.primaryTitle}
            </MovieLink>
          </Typography>
          <Typography
            sx={{
              marginLeft: 0.6,
              marginBottom: 1.4,
              color: colors.grey[400],
              fontSize: 12,
            }}
          >
            {movie.startYear +
              " - " +
              snakeToPascalCase(
                movie.movieType !== undefined ? movie.movieType : ""
              )}
          </Typography>
          <Box sx={{ display: "flex", alignItems: "center" }}>
            <Stack
              direction="row"
              spacing={1}
              sx={{ margin: 0.6, marginBottom: 3 }}
            >
              {movie.movieGenre &&
                Array.from(movie.movieGenre).map(
                  (movieGenre: MovieMovieGenreEnum) => (
                    <Item>{snakeToPascalCase(movieGenre)}</Item>
                  )
                )}
            </Stack>
          </Box>
        </Box>
        <Box sx={{ display: "flex", flexDirection: "column" }}>
          <Box>
            <Typography sx={{ fontSize: 15, margin: 1 }}>
              {movie.imdbRating}
            </Typography>
          </Box>
          <Box>
            <Typography sx={{ fontSize: 15, margin: 1 }}>
              {movie.runtimeMinutes != null
                ? movie.runtimeMinutes + " min"
                : movie.runtimeMinutes}
            </Typography>
          </Box>
        </Box>
      </Card>
    </div>
  );
};

export default MovieCard;
