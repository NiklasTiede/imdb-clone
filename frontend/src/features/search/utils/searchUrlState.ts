import {
  MovieSearchRequest,
  MovieSearchRequestMovieGenreEnum,
  MovieSearchRequestMovieTypeEnum,
} from "../../../client/movies/generator-output";

export type SearchUrlState = {
  filters: MovieSearchRequest;
  page: number;
  query: string | null;
  sort: "rating_desc" | null;
};

export const parseSearchUrlState = (search: string): SearchUrlState => {
  const params = new URLSearchParams(search);
  const query = params.get("q") ?? params.get("query");
  const pageParam = Number.parseInt(params.get("page") ?? "1", 10);
  const genre = parseGenre(params.get("genre"));
  const movieType = parseMovieType(params.get("type"));
  const minYear = parseMinYear(params.get("minYear"));
  const sort = params.get("sort") === "rating_desc" ? "rating_desc" : null;
  const filters: MovieSearchRequest = {
    ...(genre ? { movieGenre: new Set([genre]) } : {}),
    ...(movieType ? { movieType } : {}),
    ...(minYear !== null ? { minStartYear: minYear } : {}),
  };

  return {
    filters,
    page: Number.isFinite(pageParam) && pageParam > 1 ? pageParam - 1 : 0,
    query: query?.trim() || null,
    sort,
  };
};

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

const parseMinYear = (minYear: string | null): number | null => {
  if (!minYear) {
    return null;
  }

  const parsed = Number.parseInt(minYear, 10);
  return Number.isFinite(parsed) ? parsed : null;
};
