import {
  movieDetailPath,
  movieSearchPath,
} from "../../../shared/navigation/appRoutes";
import type { ApplicationAction } from "./concierge";

const pages = {
  home: "/",
  settings: "/account-settings",
  watchlist: "/your-watchlist",
  ratings: "/your-ratings",
} as const;

export const applicationDestination = (
  action: ApplicationAction,
  signedIn: boolean,
): string => {
  switch (action.type) {
    case "open_page":
      return action.destination !== "home" && !signedIn
        ? "/login"
        : pages[action.destination];
    case "open_movie_trailer":
      return `${movieDetailPath(action.movieId)}#trailer`;
    case "open_movie":
      return movieDetailPath(action.movieId);
    case "open_watchlist":
      return signedIn ? pages.watchlist : "/login";
    case "open_ratings":
      return signedIn ? pages.ratings : "/login";
    case "open_login":
      return "/login";
    case "show_search_results":
      return movieSearchPath(action.query, {
        movieGenre: new Set(action.genres),
        ...(action.movieType != null ? { movieType: action.movieType } : {}),
        ...(action.minStartYear != null
          ? { minStartYear: action.minStartYear }
          : {}),
        ...(action.maxStartYear != null
          ? { maxStartYear: action.maxStartYear }
          : {}),
        ...(action.minRuntimeMinutes != null
          ? { minRuntimeMinutes: action.minRuntimeMinutes }
          : {}),
        ...(action.maxRuntimeMinutes != null
          ? { maxRuntimeMinutes: action.maxRuntimeMinutes }
          : {}),
      });
  }
};
