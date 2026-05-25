import { expect, test } from "@playwright/test";

const transparentPixel =
  "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAqf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/ASP/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/ASP/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAY/Aqf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/ISP/2gAMAwEAAgADAAAAEP/EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QH//EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QH//EFBABAQAAAAAAAAAAAAAAAAAAABD/2gAIAQEAAT8QH//Z";

const watchlistedMovie = {
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

test("redirects anonymous users from protected routes to login", async ({
  page,
}) => {
  await page.goto("/your-watchlist");

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
});

test("renders protected watchlist for authenticated users", async ({
  page,
}) => {
  await page.addInitScript(() => {
    window.localStorage.setItem("jwtExpiresAt", String(Date.now() / 1000 + 60));
    window.localStorage.setItem("rolesFromJwt", "ROLE_USER");
    window.localStorage.setItem("username", "test_user");
  });
  await page.route("**/api/account/test_user/watchlist**", async (route) => {
    expect(new URL(route.request().url()).searchParams.get("size")).toBe("30");
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [
          {
            accountId: 1,
            movieId: watchlistedMovie.id,
            addedAt: "2026-05-08T12:00:00Z",
            movie: watchlistedMovie,
          },
        ],
        page: 0,
        size: 30,
        totalElements: 1,
        totalPages: 1,
        last: true,
      }),
    });
  });
  await page.route("**/imdb-clone/movies/*.jpg", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });

  await page.goto("/your-watchlist");

  await expect(page).toHaveURL(/\/your-watchlist$/);
  await expect(
    page.getByRole("heading", { name: "Your watchlist" }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: "It Follows" })).toBeVisible();
  await expect(page.getByAltText("movie poster")).toHaveAttribute(
    "src",
    /itFollowsPosterToken_size_300x450\.jpg/,
  );
  await expect(page.getByText("Movies", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Pick for me" })).toBeVisible();
});

test("renders protected ratings for authenticated users", async ({ page }) => {
  await page.addInitScript(() => {
    window.localStorage.setItem("jwtExpiresAt", String(Date.now() / 1000 + 60));
    window.localStorage.setItem("rolesFromJwt", "ROLE_USER");
    window.localStorage.setItem("username", "test_user");
  });
  await page.route("**/api/account/test_user/ratings**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [{ accountId: 1, movieId: watchlistedMovie.id, rating: 8 }],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        last: true,
      }),
    });
  });
  await page.route("**/api/movie/get-movies**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [watchlistedMovie],
        page: 0,
        size: 1,
        totalElements: 1,
        totalPages: 1,
        last: true,
      }),
    });
  });
  await page.route("**/imdb-clone/movies/*.jpg", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });

  await page.goto("/your-ratings");

  await expect(page).toHaveURL(/\/your-ratings$/);
  await expect(page.getByRole("heading", { name: "Your Ratings" })).toBeVisible();
  await expect(page.getByRole("link", { name: "It Follows" })).toBeVisible();
  await expect(page.getByLabel("Your rating 8 out of 10")).toBeVisible();
});
