import { Box, Stack, Typography } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { movieColors } from "../../../theme";
import { formatRatingCount } from "../utils/formatRatingCount";

export const IMDB_GOLD = movieColors.rating;
export const COMMUNITY_BLUE = movieColors.info;

type RatingPillProps = {
  label: string;
  score: number | null | undefined;
  count: number | null | undefined;
  starColor: string;
  sx?: SxProps<Theme>;
};

const containerSx: SxProps<Theme> = {
  backgroundColor: movieColors.surfaceElevated,
  border: "1px solid rgba(255,255,255,0.06)",
  borderRadius: 1.5,
  px: 1.5,
  py: 1,
  flex: 1,
  minWidth: 0,
};

export const RatingPill = ({
  label,
  score,
  count,
  starColor,
  sx,
}: RatingPillProps) => {
  const formattedScore =
    score === null || score === undefined ? "—" : score.toFixed(1);

  return (
    <Box sx={[containerSx, ...(Array.isArray(sx) ? sx : [sx])]}>
      <Typography
        sx={{
          fontSize: 11,
          color: "rgba(255,255,255,0.5)",
          letterSpacing: 0,
          mb: 0.4,
        }}
      >
        {label}
      </Typography>
      <Stack direction="row" spacing={0.5} sx={{ alignItems: "baseline" }}>
        <Box
          component="span"
          data-testid="rating-pill-star"
          style={{ color: starColor }}
          sx={{ fontSize: 14, lineHeight: 1 }}
        >
          ★
        </Box>
        <Typography sx={{ fontWeight: 500, fontSize: 16, lineHeight: 1.2 }}>
          {formattedScore}
        </Typography>
        <Typography
          sx={{
            color: "rgba(255,255,255,0.5)",
            fontSize: 11,
          }}
        >
          / 10 · {formatRatingCount(count)}
        </Typography>
      </Stack>
    </Box>
  );
};

export default RatingPill;
