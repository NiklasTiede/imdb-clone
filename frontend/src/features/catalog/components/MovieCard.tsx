import { Box, Card, Paper, Stack, styled, Typography } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { tokens } from "../../../theme";
import { MovieRecord } from "../../../client/movies/generator-output";
import React from "react";
import { Link } from "react-router";
import { MinioImageSize, PosterImage } from "../../../shared/media";

export const MovieLink = styled(Link)`
  text-decoration: none;
  &:hover {
    color: darkgray;
  }
`;

const Item = styled(Paper)(({ theme }) => ({
  backgroundColor: "#1A2027",
  ...theme.typography.body2,
  padding: theme.spacing(0.5),
  textAlign: "center",
  fontSize: 10,
  color: theme.palette.text.secondary,
}));

export const movieCardSx = {
  display: "flex",
  width: "100%",
  maxWidth: "100%",
  minHeight: { xs: 132, sm: 112 },
  overflow: "hidden",
} satisfies SxProps<Theme>;

const posterSx = {
  width: { xs: 82, sm: 80 },
  minWidth: { xs: 82, sm: 80 },
  height: { xs: 123, sm: 120 },
  p: 1,
} satisfies SxProps<Theme>;

export function snakeToPascalCase(str: string): string {
  const camelCase = str
    .toLowerCase()
    .replace(/_(\w)/g, (_, letter) => letter.toUpperCase());
  return camelCase.charAt(0).toUpperCase() + camelCase.slice(1);
}

const MovieCard = (movie: MovieRecord) => {
  const colors = tokens();

  return (
    <Card sx={movieCardSx}>
      <PosterImage
        imageUrlToken={movie.imageUrlToken}
        size={MinioImageSize.Small}
        sx={posterSx}
      />
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          flex: 1,
          minWidth: 0,
        }}
      >
        <Typography
          sx={{
            m: 0.7,
            fontSize: 17,
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
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
            mb: { xs: 0.5, sm: 1.4 },
            color: colors.grey[400],
            fontSize: 12,
          }}
        >
          {movie.startYear +
            " - " +
            snakeToPascalCase(
              movie.movieType !== undefined ? movie.movieType : "",
            )}
        </Typography>
        <Box sx={{ display: "flex", alignItems: "center" }}>
          <Stack
            direction="row"
            spacing={0.75}
            useFlexGap
            sx={{
              m: 0.6,
              mb: { xs: 1, sm: 2 },
              flexWrap: "wrap",
              overflow: "hidden",
            }}
          >
            {movie.movieGenre &&
              Array.from(movie.movieGenre).map((movieGenre) => (
                <Item key={String(movieGenre)}>
                  {snakeToPascalCase(String(movieGenre))}
                </Item>
              ))}
          </Stack>
        </Box>
      </Box>
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "flex-end",
          minWidth: { xs: 54, sm: 68 },
        }}
      >
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
  );
};

export default MovieCard;
