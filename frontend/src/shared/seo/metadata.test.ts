import { createSeoAssets, getPageMetadata } from "./metadata";

describe("page metadata", () => {
  it("keeps movie identities distinct while removing tracking parameters", () => {
    expect(
      getPageMetadata(
        "https://popcornsociety.app",
        "/movie",
        "?id=123&utm_source=mail",
      ).canonical,
    ).toBe("https://popcornsociety.app/movie?id=123");
    expect(
      getPageMetadata("https://popcornsociety.app", "/movie", "?id=456")
        .canonical,
    ).toBe("https://popcornsociety.app/movie?id=456");
  });

  it("keeps the legacy site's canonical on its own host until cutover", () => {
    expect(
      getPageMetadata(
        "https://imdb-clone.the-coding-lab.com",
        "/",
        "?utm_source=cv",
      ).canonical,
    ).toBe("https://imdb-clone.the-coding-lab.com/");
  });

  it.each([
    "/reset-password",
    "/confirm-email",
    "/account-settings",
    "/your-watchlist",
    "/movie-search",
    "/unknown",
  ])(
    "does not expose private URLs or search variants in metadata: %s",
    (path) => {
      const metadata = getPageMetadata(
        "https://popcornsociety.app",
        path,
        "?token=secret&q=private",
      );
      expect(metadata.robots).toBe("noindex, follow");
      expect(metadata.canonical).toBeNull();
    },
  );

  it.each(["", "?id=-1", "?id=abc", "?id=1&id=2"])(
    "does not index invalid movie URLs: %s",
    (search) => {
      expect(
        getPageMetadata("https://popcornsociety.app", "/movie", search).robots,
      ).toBe("noindex, follow");
    },
  );
});

describe("generated crawl files", () => {
  it("points robots and the homepage sitemap at the chosen release domain", () => {
    const assets = createSeoAssets("https://popcornsociety.app/");
    expect(assets["robots.txt"]).toContain(
      "Sitemap: https://popcornsociety.app/sitemap.xml",
    );
    expect(assets["sitemap.xml"]).toContain(
      "<loc>https://popcornsociety.app/</loc>",
    );
    expect(assets["sitemap.xml"]).not.toContain("movie-search");
  });

  it.each([
    "https://example.com/path",
    "https://example.com/?token=secret",
    "https://user:password@example.com",
    "http://example.com",
  ])("rejects a malformed production origin: %s", (origin) => {
    expect(() => createSeoAssets(origin)).toThrow();
  });
});
