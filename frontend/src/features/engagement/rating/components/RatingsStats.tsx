import StarIcon from "@mui/icons-material/Star";
import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { movieColors } from "../../../../theme";
import type { RatedMovie } from "../api/ratingQueries";
import { buildRatingsStats } from "../utils/ratingsStats";

type RatingsStatsProps = {
  items: RatedMovie[];
};

const RatingsStats = ({ items }: RatingsStatsProps) => {
  const stats = buildRatingsStats(items);
  const tiles = [
    { label: "Rated", value: String(stats.movieCount) },
    {
      label: "Your average",
      value: stats.averageRating,
      icon: <StarIcon sx={{ color: "#4dabf7", fontSize: 18 }} />,
    },
    { label: "Top genre", value: stats.topGenre },
    { label: "Top decade", value: stats.topDecade },
  ];

  if (items.length === 0) {
    return null;
  }

  return (
    <Box
      sx={{
        display: "grid",
        gap: 1.25,
        gridTemplateColumns: {
          xs: "repeat(2, minmax(0, 1fr))",
          md: "repeat(4, minmax(0, 1fr))",
        },
      }}
    >
      {tiles.map((tile) => (
        <Paper
          key={tile.label}
          variant="outlined"
          sx={{
            backgroundColor: movieColors.surfaceElevated,
            borderColor: "divider",
            borderRadius: 1,
            p: 1.5,
          }}
        >
          <Typography
            sx={{
              color: "text.secondary",
              fontSize: 11,
              letterSpacing: 0,
              textTransform: "uppercase",
            }}
          >
            {tile.label}
          </Typography>
          <Box
            sx={{ alignItems: "center", display: "flex", gap: 0.5, mt: 0.5 }}
          >
            {tile.icon}
            <Typography sx={{ fontSize: 18, fontWeight: 600 }}>
              {tile.value}
            </Typography>
          </Box>
        </Paper>
      ))}
    </Box>
  );
};

export default RatingsStats;
