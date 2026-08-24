export const movieDetailPath = (movieId: number): string => {
  if (!Number.isSafeInteger(movieId) || movieId <= 0) {
    throw new Error("A movie route requires a positive catalog ID.");
  }
  return `/movie?id=${movieId}`;
};
