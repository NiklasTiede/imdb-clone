import { movieColors } from "../../../../theme";

const ratingBadgeSizeSx = {
  borderRadius: 0.75,
  bottom: 6,
  display: "inline-flex",
  fontSize: 11,
  fontWeight: 600,
  gap: 0.25,
  px: 0.75,
  py: 0.25,
  position: "absolute",
};

export const yourRatingBadgeSx = {
  alignItems: "center",
  backgroundColor: "rgba(5,10,20,0.72)",
  border: "1px solid rgba(77,171,247,0.32)",
  color: "rgba(255,255,255,0.86)",
  left: 6,
  ...ratingBadgeSizeSx,
};

export const yourRatingStarSx = {
  color: "rgba(77,171,247,0.9)",
  fontSize: 13,
};

export const imdbRatingBadgeSx = {
  alignItems: "center",
  backgroundColor: "rgba(0,0,0,0.75)",
  color: "rgba(255,255,255,0.82)",
  right: 6,
  ...ratingBadgeSizeSx,
};

export const imdbRatingStarSx = {
  color: movieColors.gold,
  fontSize: 13,
};
