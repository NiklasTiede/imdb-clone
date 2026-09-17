import { Box, Stack, Typography } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { formatRatingCount } from "../utils/formatRatingCount";

type RatingPillProps = {
  label: string;
  score: number | null | undefined;
  count: number | null | undefined;
  sx?: SxProps<Theme>;
};

const containerSx: SxProps<Theme> = {
  backgroundColor: (theme) => theme.alpha(theme.palette.surface.raised, 0.86),
  border: "1px solid",
  borderColor: "divider",
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

export const RatingPill = ({ label, score, count, sx }: RatingPillProps) => {
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
          color: "text.secondary",
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
          sx={{ color: "star", fontSize: 14, lineHeight: 1 }}
        >
          ★
        </Box>
        <Typography
          sx={{
            fontSize: 18,
            fontVariantNumeric: "tabular-nums",
            fontWeight: 600,
            lineHeight: 1.2,
          }}
        >
          {formattedScore}
        </Typography>
        <Typography
          sx={{
            color: "text.secondary",
            fontVariantNumeric: "tabular-nums",
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
