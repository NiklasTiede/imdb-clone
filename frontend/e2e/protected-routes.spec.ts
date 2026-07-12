import { expect, type Page, test } from "@playwright/test";

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

const mockAnonymousSession = async (page: Page) => {
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({ status: 401, body: "" });
  });
};

const mockAuthenticatedSession = async (page: Page) => {
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        id: 1,
        username: "test_user",
        email: "test@example.com",
        roles: ["ROLE_USER"],
      }),
    });
  });
  await page.route("**/api/account/me/profile", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        username: "test_user",
        email: "test@example.com",
      }),
    });
  });
};

test("redirects anonymous users from protected routes to login", async ({
  page,
}) => {
  await mockAnonymousSession(page);

  await page.goto("/your-watchlist");

  await expect(page).toHaveURL(/\/login$/);
  await expect(
    page.getByRole("button", { name: "Sign in", exact: true }),
  ).toBeVisible();
});

test("renders protected watchlist for authenticated users", async ({
  page,
}) => {
  await mockAuthenticatedSession(page);
  await page.route(
    "**/api/account/test_user/library/watchlist**",
    async (route) => {
      expect(new URL(route.request().url()).searchParams.get("size")).toBe(
        "30",
      );
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          items: {
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
          },
          insights: {
            totalMovies: 1,
            totalRuntimeMinutes: 100,
            averageImdbRating: 6.8,
            topGenres: [{ label: "Horror", movieCount: 1 }],
            quickWatchCount: 1,
          },
        }),
      });
    },
  );
  await page.route("**/imdb-clone/movies/**", async (route) => {
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
    /itFollowsPosterToken_size_300x450\.webp/,
  );
  await expect(
    page.getByRole("button", { name: "Recommend something new" }),
  ).toBeVisible();
});

test("renders protected ratings for authenticated users", async ({ page }) => {
  await mockAuthenticatedSession(page);
  await page.route(
    "**/api/account/test_user/library/ratings**",
    async (route) => {
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          items: {
            content: [
              {
                accountId: 1,
                movieId: watchlistedMovie.id,
                rating: 8,
                movie: watchlistedMovie,
              },
            ],
            page: 0,
            size: 30,
            totalElements: 1,
            totalPages: 1,
            last: true,
          },
          insights: {
            totalRatings: 1,
            averageUserRating: 8,
            distribution: [{ label: "8–8.9", count: 1 }],
            favoriteGenres: [],
            favoriteDecades: [],
            definingMovies: [],
          },
        }),
      });
    },
  );
  await page.route("**/imdb-clone/movies/**", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });

  await page.goto("/your-ratings");

  await expect(page).toHaveURL(/\/your-ratings$/);
  await expect(
    page.getByRole("heading", { name: "Your Ratings" }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: "It Follows" })).toBeVisible();
  await expect(page.getByLabel("Your rating 8 out of 10")).toBeVisible();
});
