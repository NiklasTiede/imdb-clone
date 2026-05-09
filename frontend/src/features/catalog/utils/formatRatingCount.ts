export const formatRatingCount = (count: number | null | undefined): string => {
  if (count === null || count === undefined) {
    return "—";
  }

  if (count >= 1_000_000) {
    return stripTrailingZero(count / 1_000_000) + "M";
  }

  if (count >= 1000) {
    return stripTrailingZero(count / 1000) + "k";
  }

  return String(count);
};

const stripTrailingZero = (value: number): string => {
  const rounded = Math.round(value * 10) / 10;
  return rounded % 1 === 0 ? String(Math.trunc(rounded)) : rounded.toFixed(1);
};
