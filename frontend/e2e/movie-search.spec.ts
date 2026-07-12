import { expect, type Page, test } from "@playwright/test";

const transparentPixel =
  "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAqf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/ASP/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/ASP/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAY/Aqf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/ISP/2gAMAwEAAgADAAAAEP/EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QH//EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QH//EFBABAQAAAAAAAAAAAAAAAAAAABD/2gAIAQEAAT8QH//Z";

const nightcrawler = {
  id: 2872718,
  movieType: "MOVIE",
  primaryTitle: "Nightcrawler",
  originalTitle: "Nightcrawler",
  adult: false,
  startYear: 2014,
  endYear: null,
  runtimeMinutes: 117,
  movieGenre: ["CRIME", "DRAMA", "THRILLER"],
  imdbRating: 7.8,
  imdbRatingCount: 528339,
  description: "A driven freelancer enters the world of crime journalism.",
  posterImageToken: "9BGAIYNfdY90aIkV66dIJ6Olee7JGn",
};

const itFollows = {
  id: 3235888,
  movieType: "MOVIE",
  primaryTitle: "It Follows",
  originalTitle: "It Follows",
  adult: false,
  startYear: 2014,
  endYear: null,
  runtimeMinutes: 100,
  movieGenre: ["HORROR", "MYSTERY", "THRILLER"],
  imdbRating: 6.8,
  imdbRatingCount: 293479,
  description: "A young woman is followed by an unknown supernatural force.",
  posterImageToken: "itFollowsPosterToken",
};

async function stubNightcrawlerSearch(page: Page) {
  await page.route("**/api/search/movies**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [nightcrawler],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        last: true,
      }),
    });
  });
}

async function stubItFollowsSearch(page: Page) {
  let requestedQuery: string | null = null;

  await page.route("**/api/search/movies**", async (route) => {
    requestedQuery = new URL(route.request().url()).searchParams.get("query");
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [itFollows],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        last: true,
      }),
    });
  });

  return {
    expectRequestedQuery: () => expect(requestedQuery).toBe("it follows"),
  };
}

async function stubMoviePosters(page: Page) {
  await page.route("**/imdb-clone/movies/**", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });
}

async function stubGenreSearch(page: Page) {
  const requestedGenres: Array<string | null> = [];

  await page.route("**/api/search/movies**", async (route) => {
    const body = route.request().postDataJSON() as {
      movieGenre?: string[];
    };
    const genre = body.movieGenre?.[0] ?? null;
    requestedGenres.push(genre);
    const movie = genre === "HORROR" ? itFollows : nightcrawler;

    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [movie],
        last: true,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    });
  });

  return { requestedGenres };
}

test.beforeEach(async ({ page }) => {
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({ status: 401, body: "" });
  });
});

test("searches movies and renders a seeded poster", async ({ page }) => {
  await stubNightcrawlerSearch(page);
  await stubMoviePosters(page);

  await page.goto("/");
  await expect(page.getByRole("textbox", { name: "search" })).toBeVisible();

  await page.getByRole("textbox", { name: "search" }).fill("Nightcrawler");
  await page.getByRole("textbox", { name: "search" }).press("Enter");

  await expect(page).toHaveURL(/\/movie-search\?query=Nightcrawler$/);
  await expect(
    page.getByRole("heading", { name: 'Results for "Nightcrawler"' }),
  ).toBeVisible();
  await expect(page.getByText("1 movie")).toBeVisible();
  const searchResults = page.getByRole("grid", { name: "Search results" });
  await expect(searchResults).toBeVisible();
  await expect(
    searchResults.getByRole("link", { name: "Nightcrawler" }),
  ).toBeVisible();
  await expect(page.getByText("7.8")).toBeVisible();
  await expect(page.getByText("2014 · 117 min")).toBeVisible();
  await expect(page.getByAltText("movie poster")).toHaveAttribute(
    "src",
    /9BGAIYNfdY90aIkV66dIJ6Olee7JGn_size_300x450\.webp/,
  );
});

test("searches for a multi-word lowercase movie title", async ({ page }) => {
  const searchRequest = await stubItFollowsSearch(page);
  await stubMoviePosters(page);

  await page.goto("/");
  await page.getByRole("textbox", { name: "search" }).fill("it follows");
  await page.getByRole("textbox", { name: "search" }).press("Enter");

  await expect(page).toHaveURL(/\/movie-search\?query=it%20follows$/);
  await expect(
    page.getByRole("heading", { name: 'Results for "it follows"' }),
  ).toBeVisible();
  const searchResults = page.getByRole("grid", { name: "Search results" });
  await expect(searchResults).toBeVisible();
  await expect(
    searchResults.getByRole("link", { name: "It Follows" }),
  ).toBeVisible();
  await expect(page.getByAltText("movie poster")).toHaveAttribute(
    "src",
    /itFollowsPosterToken_size_300x450\.webp/,
  );
  searchRequest.expectRequestedQuery();
});

test("keeps every character while earlier search results load", async ({ page }) => {
  await page.route("**/api/search/movies**", async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 450));
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [nightcrawler],
        last: true,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    });
  });
  await stubMoviePosters(page);

  await page.goto("/");
  const searchInput = page.getByRole("textbox", { name: "search movies" });

  await searchInput.fill("spir");
  await expect(page).toHaveURL(/query=spir/);
  await searchInput.pressSequentially("ited", { delay: 55 });

  await expect(searchInput).toHaveValue("spirited");
  await expect(page).toHaveURL(/query=spirited/);
  await page.waitForTimeout(500);
  await expect(searchInput).toHaveValue("spirited");
});

test("switches search results into the compact editorial list", async ({ page }) => {
  await stubNightcrawlerSearch(page);
  await stubMoviePosters(page);

  await page.goto("/movie-search?query=Nightcrawler");
  await page.getByRole("button", { name: "List view" }).click();

  const searchResults = page.getByRole("list", { name: "Search results" });
  await expect(searchResults).toBeVisible();
  await expect(
    searchResults.getByRole("link", { name: "Nightcrawler" }),
  ).toBeVisible();
  await expect(searchResults.getByText("2014 · 117 min")).toBeVisible();
  await expect(searchResults.getByLabel("IMDb rating 7.8")).toBeVisible();
  await expect(page.getByRole("grid", { name: "Search results" })).toHaveCount(0);
});

test("opens a movie detail page from search results", async ({ page }) => {
  await stubNightcrawlerSearch(page);
  await stubMoviePosters(page);
  await page.route("**/api/movie/2872718", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(nightcrawler),
    });
  });

  await page.goto("/");
  await page.getByRole("textbox", { name: "search" }).fill("Nightcrawler");
  await page.getByRole("textbox", { name: "search" }).press("Enter");
  await page
    .getByRole("grid", { name: "Search results" })
    .getByRole("link", { name: "Nightcrawler" })
    .click();

  await expect(page).toHaveURL(/\/movie\?id=2872718$/);
  await expect(
    page.getByRole("heading", { name: "Nightcrawler" }),
  ).toBeVisible();
  await expect(page.getByText("2014")).toBeVisible();
  await expect(page.getByText("1h 57m")).toBeVisible();
  await expect(page.getByText("Crime", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("IMDb rating")).toBeVisible();
  await expect(page.getByText("7.8")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Synopsis" })).toBeVisible();
  await expect(
    page.getByText("A driven freelancer enters the world of crime journalism."),
  ).toBeVisible();
  await expect(page.getByAltText("Nightcrawler poster")).toHaveAttribute(
    "src",
    /9BGAIYNfdY90aIkV66dIJ6Olee7JGn_size_600x900\.webp/,
  );
});

test("replaces genre filters and fetches fresh results", async ({ page }, testInfo) => {
  const search = await stubGenreSearch(page);
  await stubMoviePosters(page);
  const isMobile = testInfo.project.name === "mobile-chromium";

  await page.goto("/movie-search?query=the");
  if (isMobile) {
    await page.getByRole("button", { name: "Filters" }).click();
    await page.getByRole("button", { name: "Horror", exact: true }).click();
    await page.getByRole("button", { name: "Show results" }).click();
  } else {
    await page.getByRole("button", { name: "All genres" }).click();
    await page.getByRole("menuitem", { name: "Horror" }).click();
  }

  await expect(page).toHaveURL(/genre=HORROR/);
  await expect(
    page
      .getByRole("grid", { name: "Search results" })
      .getByRole("link", { name: /It Follows/ }),
  ).toBeVisible();

  if (isMobile) {
    await page.getByRole("button", { name: "Filters" }).click();
    await page.getByRole("button", { name: "Drama", exact: true }).click();
    await page.getByRole("button", { name: "Show results" }).click();
  } else {
    await page.locator("button").filter({ hasText: "Horror" }).click();
    await page.getByRole("menuitem", { name: "Drama" }).click();
  }

  await expect(page).toHaveURL(/genre=DRAMA/);
  await expect(
    page
      .getByRole("grid", { name: "Search results" })
      .getByRole("link", { name: /Nightcrawler/ }),
  ).toBeVisible();
  expect(search.requestedGenres).toContain("HORROR");
  expect(search.requestedGenres).toContain("DRAMA");
});

test("uses the compact mobile filter drawer", async ({ page }, testInfo) => {
  test.skip(
    testInfo.project.name !== "mobile-chromium",
    "The mobile drawer is only visible in the mobile project.",
  );
  await stubGenreSearch(page);
  await stubMoviePosters(page);

  await page.goto("/movie-search?query=the");
  await page.getByRole("button", { name: "Filters" }).click();

  await expect(page.getByText("Filter movies")).toBeVisible();
  await page.getByRole("button", { name: "Horror", exact: true }).click();
  await page.getByRole("button", { name: "Show results" }).click();

  await expect(page).toHaveURL(/genre=HORROR/);
  await expect(
    page
      .getByRole("grid", { name: "Search results" })
      .getByRole("link", { name: /It Follows/ }),
  ).toBeVisible();
});
