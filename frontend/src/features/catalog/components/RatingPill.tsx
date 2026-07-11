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
  backgroundColor: "rgba(23,33,50,0.86)",
  border: "1px solid rgba(255,255,255,0.1)",
  backdropFilter: "blur(10px)",
  borderRadius: 1,
  px: 1.5,
  py: 1.25,
  flex: 1,
  minWidth: 0,
};

type SxArray = Extract<SxProps<Theme>, readonly unknown[]>;

const isSxArray = (value: SxProps<Theme> | undefined): value is SxArray =>
  Array.isArray(value);

export const RatingPill = ({
  label,
  score,
  count,
  starColor,
  sx,
}: RatingPillProps) => {
  const formattedScore =
    score === null || score === undefined ? "—" : score.toFixed(1);
  const combinedSx: SxProps<Theme> = isSxArray(sx)
    ? [containerSx, ...sx]
    : sx === undefined
      ? [containerSx]
      : [containerSx, sx];

  return (
    <Box sx={combinedSx}>
      <Typography
        sx={{
          fontSize: 11,
          color: "rgba(255,255,255,0.62)",
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
        <Typography sx={{ fontWeight: 600, fontSize: 18, lineHeight: 1.2 }}>
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
