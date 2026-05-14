import Box from "@mui/material/Box";
import type { WatchlistItem } from "../model/watchlist";
import WatchlistMovieCard from "./WatchlistMovieCard";

type WatchlistGridProps = {
  items: WatchlistItem[];
  onRemove: (movieId: number) => void;
};

export const watchlistGridSx = {
  display: "grid",
  gap: { xs: 1.5, sm: 2 },
  gridTemplateColumns: {
    xs: "repeat(2, minmax(0, 1fr))",
    sm: "repeat(auto-fill, minmax(150px, 180px))",
    md: "repeat(auto-fill, minmax(160px, 190px))",
  },
  justifyContent: "start",
};

const WatchlistGrid = ({ items, onRemove }: WatchlistGridProps) => (
  <Box aria-label="Watchlist movies" role="grid" sx={watchlistGridSx}>
    {items.map((item) => (
      <Box key={item.movieId} role="gridcell">
        <WatchlistMovieCard item={item} onRemove={onRemove} />
      </Box>
    ))}
  </Box>
);

export default WatchlistGrid;
