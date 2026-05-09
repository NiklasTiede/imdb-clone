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
  imageUrlToken: "9BGAIYNfdY90aIkV66dIJ6Olee7JGn",
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
  imageUrlToken: "itFollowsPosterToken",
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
  await page.route("**/imdb-clone/movies/*.jpg", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });
}

test("searches movies and renders a seeded poster", async ({ page }) => {
  await stubNightcrawlerSearch(page);
  await stubMoviePosters(page);

  await page.goto("/");
  await expect(page.getByText("Home Page")).toBeVisible();

  await page.getByRole("textbox", { name: "search" }).fill("Nightcrawler");
  await page.getByRole("textbox", { name: "search" }).press("Enter");

  await expect(page).toHaveURL(/\/movie-search\?query=Nightcrawler$/);
  await expect(page.getByRole("link", { name: "Nightcrawler" })).toBeVisible();
  await expect(page.getByAltText("movie poster")).toHaveAttribute(
    "src",
    /9BGAIYNfdY90aIkV66dIJ6Olee7JGn_size_120x180\.jpg/,
  );
});

test("searches for a multi-word lowercase movie title", async ({ page }) => {
  const searchRequest = await stubItFollowsSearch(page);
  await stubMoviePosters(page);

  await page.goto("/");
  await page.getByRole("textbox", { name: "search" }).fill("it follows");
  await page.getByRole("textbox", { name: "search" }).press("Enter");

  await expect(page).toHaveURL(/\/movie-search\?query=it%20follows$/);
  await expect(page.getByRole("link", { name: "It Follows" })).toBeVisible();
  await expect(page.getByAltText("movie poster")).toHaveAttribute(
    "src",
    /itFollowsPosterToken_size_120x180\.jpg/,
  );
  searchRequest.expectRequestedQuery();
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
  await page.getByRole("link", { name: "Nightcrawler" }).click();

  await expect(page).toHaveURL(/\/movie\?id=2872718$/);
  await expect(
    page.getByRole("heading", { name: "Nightcrawler" }),
  ).toBeVisible();
  await expect(page.getByText("2014")).toBeVisible();
  await expect(page.getByText("117 min")).toBeVisible();
  await expect(page.getByText("Crime", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("IMDb rating")).toBeVisible();
  await expect(page.getByText("7.8")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Synopsis" })).toBeVisible();
  await expect(
    page.getByText("A driven freelancer enters the world of crime journalism."),
  ).toBeVisible();
  await expect(page.getByAltText("movie poster")).toHaveAttribute(
    "src",
    /9BGAIYNfdY90aIkV66dIJ6Olee7JGn_size_600x900\.jpg/,
  );
});
