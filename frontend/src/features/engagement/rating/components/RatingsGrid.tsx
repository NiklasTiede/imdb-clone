import Box from "@mui/material/Box";
import { watchlistGridSx } from "../../watchlist/components/WatchlistGrid";
import type { RatedMovie } from "../api/ratingQueries";
import RatingsMovieCard from "./RatingsMovieCard";

type RatingsGridProps = {
  items: RatedMovie[];
  onRemove?: (movieId: number) => void;
};

const RatingsGrid = ({ items, onRemove }: RatingsGridProps) => (
  <Box aria-label="Rated movies" role="grid" sx={watchlistGridSx}>
    {items.map((item) => (
      <Box key={item.movie.id ?? item.movie.primaryTitle} role="gridcell">
        <RatingsMovieCard item={item} onRemove={onRemove} />
      </Box>
    ))}
  </Box>
);

export default RatingsGrid;
