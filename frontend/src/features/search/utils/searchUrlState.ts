import {
  MovieSearchRequest,
  MovieSearchRequestMovieGenreEnum,
  MovieSearchRequestMovieTypeEnum,
} from "../../../client/movies/generator-output";

export type SearchUrlState = {
  filters: MovieSearchRequest;
  page: number;
  query: string | null;
  sort: SearchSort;
};

export type SearchSort = "relevance" | "rating_desc";

export type SearchUrlPatch = {
  genre?: MovieSearchRequestMovieGenreEnum | null;
  maxRuntime?: number | null;
  maxYear?: number | null;
  minRuntime?: number | null;
  minYear?: number | null;
  page?: number;
  sort?: SearchSort;
  type?: MovieSearchRequestMovieTypeEnum | null;
};

export const parseSearchUrlState = (search: string): SearchUrlState => {
  const params = new URLSearchParams(search);
  const query = params.get("q") ?? params.get("query");
  const pageParam = Number.parseInt(params.get("page") ?? "1", 10);
  const genre = parseGenre(params.get("genre"));
  const movieType = parseMovieType(params.get("type"));
  const minYear = parseMinYear(params.get("minYear"));
  const maxYear = parseNumberParam(params.get("maxYear"));
  const minRuntime = parseNumberParam(params.get("minRuntime"));
  const maxRuntime = parseNumberParam(params.get("maxRuntime"));
  const sort = parseSort(params.get("sort"));
  const filters: MovieSearchRequest = {
    ...(genre ? { movieGenre: new Set([genre]) } : {}),
    ...(movieType ? { movieType } : {}),
    ...(minYear !== null ? { minStartYear: minYear } : {}),
    ...(maxYear !== null ? { maxStartYear: maxYear } : {}),
    ...(minRuntime !== null ? { minRuntimeMinutes: minRuntime } : {}),
    ...(maxRuntime !== null ? { maxRuntimeMinutes: maxRuntime } : {}),
  };

  return {
    filters,
    page: Number.isFinite(pageParam) && pageParam > 1 ? pageParam - 1 : 0,
    query: query?.trim() || null,
    sort,
  };
};

export const createSearchUrl = (
  currentSearch: string,
  patch: SearchUrlPatch,
): string => {
  const params = new URLSearchParams(currentSearch);
  const updatesPagination = patch.page !== undefined;

  setEnumParam(params, "genre", patch.genre);
  setEnumParam(params, "type", patch.type);
  setNumberParam(params, "minYear", patch.minYear);
  setNumberParam(params, "maxYear", patch.maxYear);
  setNumberParam(params, "minRuntime", patch.minRuntime);
  setNumberParam(params, "maxRuntime", patch.maxRuntime);

  if (patch.sort !== undefined) {
    if (patch.sort === "relevance") {
      params.delete("sort");
    } else {
      params.set("sort", patch.sort);
    }
  }

  if (updatesPagination) {
    if (patch.page && patch.page > 1) {
      params.set("page", String(patch.page));
    } else {
      params.delete("page");
    }
  } else if (hasFilterOrSortPatch(patch)) {
    params.delete("page");
  }

  const next = params.toString();
  return next ? `?${next}` : "";
};

const hasFilterOrSortPatch = (patch: SearchUrlPatch): boolean =>
  patch.genre !== undefined ||
  patch.type !== undefined ||
  patch.minYear !== undefined ||
  patch.maxYear !== undefined ||
  patch.minRuntime !== undefined ||
  patch.maxRuntime !== undefined ||
  patch.sort !== undefined;

const setEnumParam = (
  params: URLSearchParams,
  key: string,
  value?: string | null,
) => {
  if (value === undefined) {
    return;
  }
  if (value === null || value === "") {
    params.delete(key);
    return;
  }
  params.set(key, value);
};

const setNumberParam = (
  params: URLSearchParams,
  key: string,
  value?: number | null,
) => {
  if (value === undefined) {
    return;
  }
  if (value === null || !Number.isFinite(value)) {
    params.delete(key);
    return;
  }
  params.set(key, String(value));
};

const parseSort = (sort: string | null): SearchSort =>
  sort === "rating_desc" ? "rating_desc" : "relevance";

const parseMovieType = (
  movieType: string | null,
): MovieSearchRequestMovieTypeEnum | null => {
  if (!movieType) {
    return null;
  }

  const movieTypes = Object.values(MovieSearchRequestMovieTypeEnum);
  return movieTypes.includes(movieType as MovieSearchRequestMovieTypeEnum)
    ? (movieType as MovieSearchRequestMovieTypeEnum)
    : null;
};

const parseGenre = (
  genre: string | null,
): MovieSearchRequestMovieGenreEnum | null => {
  if (!genre) {
    return null;
  }

  const genres = Object.values(MovieSearchRequestMovieGenreEnum);
  return genres.includes(genre as MovieSearchRequestMovieGenreEnum)
    ? (genre as MovieSearchRequestMovieGenreEnum)
    : null;
};

const parseMinYear = (minYear: string | null): number | null =>
  parseNumberParam(minYear);

const parseNumberParam = (value: string | null): number | null => {
  if (!value) {
    return null;
  }

  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : null;
};
