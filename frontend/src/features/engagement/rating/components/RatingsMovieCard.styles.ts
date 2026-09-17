import type { SxProps, Theme } from "@mui/material/styles";
import { accentTint, scrimTint } from "../../../../theme";

const ratingBadgeSizeSx = {
  borderRadius: 0.75,
  bottom: 6,
  display: "inline-flex",
  fontSize: 11,
  fontVariantNumeric: "tabular-nums",
  fontWeight: 600,
  gap: 0.25,
  px: 0.75,
  py: 0.25,
  position: "absolute",
} satisfies SxProps<Theme>;

// "Your" rating carries the accent border; stars always use the star colour.
export const yourRatingBadgeSx = {
  alignItems: "center",
  backgroundColor: scrimTint(0.78),
  border: "1px solid",
  borderColor: accentTint(0.85),
  color: "text.primary",
  left: 6,
  ...ratingBadgeSizeSx,
} satisfies SxProps<Theme>;

export const yourRatingStarSx = {
  color: "star",
  fontSize: 13,
} satisfies SxProps<Theme>;

export const imdbRatingBadgeSx = {
  alignItems: "center",
  backgroundColor: scrimTint(0.78),
  color: "text.primary",
  right: 6,
  ...ratingBadgeSizeSx,
} satisfies SxProps<Theme>;

export const imdbRatingStarSx = {
  color: "star",
  fontSize: 13,
} satisfies SxProps<Theme>;
