import { MovieSearchGenre } from "../../catalog";

export type SearchRangePreset = {
  label: string;
  max?: number;
  min?: number;
};

export const SEARCHABLE_MOVIE_GENRES = [
  MovieSearchGenre.Action,
  MovieSearchGenre.Adventure,
  MovieSearchGenre.Animation,
  MovieSearchGenre.Biography,
  MovieSearchGenre.Comedy,
  MovieSearchGenre.Crime,
  MovieSearchGenre.Drama,
  MovieSearchGenre.Family,
  MovieSearchGenre.Fantasy,
  MovieSearchGenre.History,
  MovieSearchGenre.Horror,
  MovieSearchGenre.Music,
  MovieSearchGenre.Mystery,
  MovieSearchGenre.Romance,
  MovieSearchGenre.SciFi,
  MovieSearchGenre.Sport,
  MovieSearchGenre.Thriller,
  MovieSearchGenre.War,
  MovieSearchGenre.Western,
] as const;

export const YEAR_PRESETS: readonly SearchRangePreset[] = [
  { label: "Before 1990", max: 1989 },
  { label: "1990s", max: 1999, min: 1990 },
  { label: "2000s", max: 2009, min: 2000 },
  { label: "2010s", max: 2019, min: 2010 },
  { label: "2020s", min: 2020 },
];

export const RUNTIME_PRESETS: readonly SearchRangePreset[] = [
  { label: "Under 2 hours", max: 119 },
  { label: "2–2½ hours", max: 149, min: 120 },
  { label: "2½+ hours", min: 150 },
];

export const humanizeSearchValue = (value: string): string =>
  value
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

export const findRangePreset = (
  presets: readonly SearchRangePreset[],
  min: number | undefined,
  max: number | undefined,
): SearchRangePreset | undefined =>
  presets.find((preset) => preset.min === min && preset.max === max);
