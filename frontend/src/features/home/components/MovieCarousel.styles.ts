import type { SxProps, Theme } from "@mui/material/styles";

export const movieCarouselCardWidthSx = {
  xs: 130,
  sm: 150,
  md: 170,
} satisfies SxProps<Theme>;

export const movieCarouselScrollSx = {
  display: "flex",
  gap: 2,
  mx: { xs: 0, md: "-8px" },
  my: -2,
  overflowX: "auto",
  overflowY: "hidden",
  overscrollBehaviorX: "contain",
  px: { xs: 2, md: "8px" },
  py: 2,
  scrollSnapType: "x mandatory",
  scrollbarWidth: "none",
  "&::-webkit-scrollbar": { display: "none" },
} satisfies SxProps<Theme>;
