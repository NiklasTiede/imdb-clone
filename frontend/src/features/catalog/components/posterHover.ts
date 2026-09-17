import type { SxProps, Theme } from "@mui/material/styles";

export const posterHoverTargetClassName = "movie-poster-hover-target";

// The hover and focus treatment comes from the theme's effect tokens, so each
// theme decides how a poster reacts (scale and outline, or lift and light edge).
export const posterHoverContainerSx = (theme: Theme) => ({
  overflow: "visible",
  [`&:is(:hover, :focus-visible) .${posterHoverTargetClassName}`]:
    theme.effects.posterHover,
  [`&:focus-visible .${posterHoverTargetClassName}`]: theme.effects.posterFocus,
});

export const posterHoverTargetSx = {
  outline: "2px solid transparent",
  outlineOffset: 0,
  transformOrigin: "center",
  transition:
    "transform 180ms ease, outline-color 180ms ease, outline-offset 180ms ease, box-shadow 180ms ease",
  willChange: "transform",
} satisfies SxProps<Theme>;
