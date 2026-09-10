import type { Location } from "react-router";

export type PageContext = {
  streamingCountry?: string;
  page:
    | "unknown"
    | "home"
    | "movie"
    | "search"
    | "watchlist"
    | "ratings"
    | "settings"
    | "reviews"
    | "editing"
    | "login"
    | "registration";
  movieId?: number;
  searchQuery?: string;
  section?: "overview" | "trailer";
};

/** Only allowlisted location hints cross the agent boundary, never URLs or page/form contents. */
export const pageContextFromLocation = ({
  pathname,
  search,
  hash,
}: Pick<Location, "pathname" | "search" | "hash">): PageContext => {
  const params = new URLSearchParams(search);
  if (pathname === "/movie") {
    const value = params.get("id");
    const id = value && /^\d+$/.test(value) ? Number(value) : 0;
    return {
      page: "movie",
      ...(Number.isSafeInteger(id) && id > 0 ? { movieId: id } : {}),
      section: hash === "#trailer" ? "trailer" : "overview",
    };
  }
  if (pathname === "/movie-search")
    return {
      page: "search",
      searchQuery: (params.get("query") ?? "").slice(0, 200),
    };
  const pages: Record<string, PageContext["page"]> = {
    "/": "home",
    "/your-watchlist": "watchlist",
    "/your-ratings": "ratings",
    "/your-comments": "reviews",
    "/account-settings": "settings",
    "/editing": "editing",
    "/login": "login",
    "/registration": "registration",
  };
  return { page: pages[pathname] ?? "unknown" };
};
