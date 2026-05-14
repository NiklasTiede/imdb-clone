import type { Movie } from "../../../catalog";

export const formatMovieMeta = (movie: Movie): string =>
  [movie.startYear, movie.runtimeMinutes ? `${movie.runtimeMinutes} min` : null]
    .filter(Boolean)
    .join(" · ");

export const formatGenre = (genre: string): string =>
  genre
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");

export const formatRelativeDate = (value?: string): string => {
  if (!value) {
    return "Added recently";
  }

  const timestamp = new Date(value).getTime();
  if (Number.isNaN(timestamp)) {
    return "Added recently";
  }

  const diffMs = Date.now() - timestamp;
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  const week = 7 * day;
  const month = 30 * day;

  if (diffMs < hour) {
    return "Added today";
  }
  if (diffMs < day) {
    const hours = Math.max(1, Math.floor(diffMs / hour));
    return `Added ${hours} ${hours === 1 ? "hour" : "hours"} ago`;
  }
  if (diffMs < week) {
    const days = Math.max(1, Math.floor(diffMs / day));
    return `Added ${days} ${days === 1 ? "day" : "days"} ago`;
  }
  if (diffMs < month) {
    const weeks = Math.max(1, Math.floor(diffMs / week));
    return `Added ${weeks} ${weeks === 1 ? "week" : "weeks"} ago`;
  }

  const months = Math.max(1, Math.floor(diffMs / month));
  return `Added ${months} ${months === 1 ? "month" : "months"} ago`;
};
