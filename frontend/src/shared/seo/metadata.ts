export const siteName = "Popcorn Society";
export const siteDescription =
  "Discover your next movie with Popcorn Society. Explore films, save your watchlist, share ratings, and ask the Movie Concierge.";

export function getPageMetadata(
  origin: string,
  pathname: string,
  search: string,
) {
  const params = new URLSearchParams(search);
  const ids = params.getAll("id");
  const movieId =
    ids.length === 1 && /^[1-9]\d*$/.test(ids[0] ?? "") ? ids[0] : null;
  const canonical =
    pathname === "/"
      ? `${origin}/`
      : pathname === "/movie" && movieId
        ? `${origin}/movie?id=${movieId}`
        : null;

  return {
    canonical,
    robots: canonical ? "index, follow" : "noindex, follow",
  };
}

/** The initial sitemap lists the homepage; catalog URLs remain discoverable through links. */
export function createSeoAssets(siteUrl: string) {
  const url = new URL(siteUrl);
  if (
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    url.pathname !== "/" ||
    (url.protocol !== "https:" &&
      !(url.protocol === "http:" && url.hostname === "localhost"))
  ) {
    throw new Error(
      "VITE_SITE_URL must be an HTTPS origin (HTTP localhost is allowed for development).",
    );
  }
  return {
    "robots.txt": `User-agent: *\nDisallow:\n\nSitemap: ${url.origin}/sitemap.xml\n`,
    "sitemap.xml": `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n  <url><loc>${url.origin}/</loc></url>\n</urlset>\n`,
  };
}
