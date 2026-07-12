import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { movieColors } from "../../../../theme";
import { PosterMovieCard } from "../../../catalog";
import type { RatingTasteInsights } from "../model/library";

type RatingsTasteSnapshotProps = {
  insights?: RatingTasteInsights | undefined;
};

const RatingsTasteSnapshot = ({ insights }: RatingsTasteSnapshotProps) => {
  const totalRatings = insights?.totalRatings ?? 0;
  const favoriteGenre = insights?.favoriteGenres?.[0]?.label;
  const favoriteDecade = insights?.favoriteDecades?.[0]?.label;
  const maxBucketCount = Math.max(
    1,
    ...(insights?.distribution ?? []).map((bucket) => bucket.count ?? 0),
  );
  const difference = insights?.averageImdbDifference;
  const differenceCopy =
    difference === undefined
      ? "Rate more movies to compare your taste with IMDb."
      : difference === 0
        ? "Your ratings closely match IMDb on average."
        : `You rate movies ${Math.abs(difference).toFixed(1)} points ${difference > 0 ? "higher" : "lower"} than IMDb on average.`;

  return (
    <Paper
      component="section"
      variant="outlined"
      sx={{
        background: `linear-gradient(118deg, ${movieColors.surfaceElevated}, ${movieColors.surfaceInset})`,
        borderColor: "divider",
        borderRadius: 1,
        overflow: "hidden",
        p: { xs: 2, sm: 2.5 },
      }}
    >
      <Stack spacing={2}>
        <Box>
          <Typography sx={{ color: "primary.main", fontSize: 11, fontWeight: 800, letterSpacing: 1.1 }}>
            TASTE SNAPSHOT
          </Typography>
          <Typography component="h2" sx={{ fontSize: 20, fontWeight: 800, mt: 0.25 }}>
            {totalRatings < 3
              ? "Your taste profile is still forming."
              : `Your strongest signal is ${favoriteGenre ?? "still taking shape"}${favoriteDecade ? `, especially ${favoriteDecade} films` : ""}.`}
          </Typography>
          <Typography sx={{ color: "text.secondary", fontSize: 13, mt: 0.5 }}>
            {differenceCopy}
          </Typography>
        </Box>

        <Box
          sx={{
            display: "grid",
            gap: 2,
            gridTemplateColumns: { xs: "minmax(0, 1fr)", md: "minmax(0, 2fr) minmax(180px, 1fr)" },
          }}
        >
          <Box aria-label="Your rating distribution" sx={{ minWidth: 0 }}>
            <Typography sx={{ color: "text.secondary", fontSize: 11, fontWeight: 700, mb: 1, textTransform: "uppercase" }}>
              Your rating distribution
            </Typography>
            <Stack direction="row" spacing={0.75} sx={{ alignItems: "flex-end", height: 104 }}>
              {(insights?.distribution ?? []).map((bucket) => {
                const count = bucket.count ?? 0;
                return (
                  <Box key={bucket.label} sx={{ flex: 1, minWidth: 0 }}>
                    <Box
                      aria-label={`${bucket.label}: ${count} ratings`}
                      role="img"
                      sx={{
                        backgroundColor: count > 0 ? movieColors.info : "action.disabledBackground",
                        borderRadius: "4px 4px 0 0",
                        height: Math.max(4, (count / maxBucketCount) * 76),
                        transition: "height 180ms ease",
                      }}
                    />
                    <Typography align="center" noWrap sx={{ color: "text.secondary", fontSize: 10, mt: 0.5 }}>
                      {bucket.label}
                    </Typography>
                  </Box>
                );
              })}
            </Stack>
          </Box>
          <Stack spacing={1.25}>
            <SnapshotMetric label="Rated" value={String(totalRatings)} />
            <SnapshotMetric label="Your average" value={insights?.averageUserRating?.toFixed(1) ?? "—"} />
            <SnapshotMetric label="Top genre" value={favoriteGenre ?? "—"} />
            <SnapshotMetric label="Top decade" value={favoriteDecade ?? "—"} />
          </Stack>
        </Box>

        {(insights?.definingMovies?.length ?? 0) > 0 && (
          <Box>
            <Typography sx={{ color: "text.secondary", fontSize: 11, fontWeight: 700, mb: 1, textTransform: "uppercase" }}>
              Defining films
            </Typography>
            <Box sx={{ display: "flex", gap: 1.25, overflowX: "auto", pb: 0.5 }}>
              {insights?.definingMovies?.flatMap((insight) =>
                insight.movie ? [
                  <Box key={insight.movie.id} sx={{ flex: "0 0 112px" }}>
                    <PosterMovieCard movie={insight.movie} showImdbRating={false} />
                  </Box>,
                ] : [],
              )}
            </Box>
          </Box>
        )}
      </Stack>
    </Paper>
  );
};

const SnapshotMetric = ({ label, value }: { label: string; value: string }) => (
  <Box sx={{ borderLeft: "2px solid", borderColor: "primary.main", pl: 1 }}>
    <Typography sx={{ color: "text.secondary", fontSize: 10, fontWeight: 700, textTransform: "uppercase" }}>
      {label}
    </Typography>
    <Typography sx={{ fontSize: 16, fontWeight: 800 }}>{value}</Typography>
  </Box>
);

export default RatingsTasteSnapshot;
