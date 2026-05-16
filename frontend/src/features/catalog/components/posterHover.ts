import type { SxProps, Theme } from "@mui/material/styles";

export const posterHoverTargetClassName = "movie-poster-hover-target";

export const posterHoverContainerSx = {
  overflow: "visible",
  [`&:is(:hover, :focus-visible) .${posterHoverTargetClassName}`]: {
    outlineColor: "rgba(255,255,255,0.96)",
    outlineOffset: 2,
    transform: "scale(1.03)",
  },
} satisfies SxProps<Theme>;

export const posterHoverTargetSx = {
  outline: "2px solid transparent",
  outlineOffset: 0,
  transform: "scale(1)",
  transformOrigin: "center",
  transition:
    "transform 180ms ease, outline-color 180ms ease, outline-offset 180ms ease",
  willChange: "transform",
} satisfies SxProps<Theme>;
