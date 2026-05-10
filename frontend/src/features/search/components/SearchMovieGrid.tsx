import Box from "@mui/material/Box";
import type { SxProps, Theme } from "@mui/material/styles";
import { MovieRecord } from "../../../client/movies/generator-output";
import SearchMovieCard from "./SearchMovieCard";

type SearchMovieGridProps = {
  movies: MovieRecord[];
};

export const searchMovieGridSx = {
  display: "grid",
  gap: { xs: 1.5, sm: 2 },
  gridTemplateColumns: {
    xs: "repeat(2, minmax(0, 1fr))",
    sm: "repeat(auto-fill, minmax(150px, 180px))",
    md: "repeat(auto-fill, minmax(160px, 190px))",
  },
  justifyContent: "start",
} satisfies SxProps<Theme>;

const SearchMovieGrid = ({ movies }: SearchMovieGridProps) => (
  <Box
    aria-label="Search results"
    role="grid"
    sx={searchMovieGridSx}
  >
    {movies.map((movie) => (
      <Box key={movie.id} role="gridcell">
        <SearchMovieCard movie={movie} />
      </Box>
    ))}
  </Box>
);

export default SearchMovieGrid;
