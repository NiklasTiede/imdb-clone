import type { Theme } from "@mui/material/styles";

/** Translucent accent for tinted backgrounds and borders, usable as an `sx` value. */
export const accentTint = (opacity: number) => (theme: Theme) =>
  theme.alpha(theme.palette.accent.main, opacity);

/** Translucent scrim for overlays on images, usable as an `sx` value. */
export const scrimTint = (opacity: number) => (theme: Theme) =>
  theme.alpha(theme.palette.scrim, opacity);
