import { expect, type Page, test } from "@playwright/test";

const transparentPixel =
  "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAqf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/ASP/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/ASP/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAY/Aqf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/ISP/2gAMAwEAAgADAAAAEP/EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QH//EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QH//EFBABAQAAAAAAAAAAAAAAAAAAABD/2gAIAQEAAT8QH//Z";

const movie = (id: number, title: string) => ({
  id,
  imdbId: `tt00000${id}`,
  movieType: "MOVIE",
  primaryTitle: title,
  originalTitle: title,
  adult: false,
  startYear: 2000 + id,
  runtimeMinutes: 100,
  movieGenre: id === 1 ? ["SCI_FI"] : ["DRAMA"],
  imdbRating: 7.5 + id / 10,
  imdbRatingCount: 1000 * id,
  posterImageToken: `poster-${id}`,
});

const firstMovie = movie(1, "First Library Film");
const secondMovie = movie(2, "Second Library Film");
const thirdMovie = movie(3, "Third Library Film");
const fourthMovie = movie(4, "New Discovery One");
const fifthMovie = movie(5, "New Discovery Two");
const sixthMovie = movie(6, "New Discovery Three");

const mockAuthenticatedSession = async (page: Page) => {
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        id: 7,
        username: "niklas",
        email: "niklas@example.com",
        roles: ["ROLE_USER"],
      }),
    });
  });
  await page.route("**/api/account/me/profile", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ username: "niklas", email: "niklas@example.com" }),
    });
  });
  await page.route("**/imdb-clone/movies/**", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });
};

test("loads complete ratings insights while browsing additional pages", async ({
  page,
}) => {
  await mockAuthenticatedSession(page);
  await page.route("**/api/account/niklas/library/ratings**", async (route) => {
    const requestedPage = Number(
      new URL(route.request().url()).searchParams.get("page") ?? "0",
    );
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        items: {
          content:
            requestedPage === 0
              ? [{ accountId: 7, movieId: 1, rating: 9, movie: firstMovie }]
              : [{ accountId: 7, movieId: 2, rating: 7, movie: secondMovie }],
          page: requestedPage,
          size: 30,
          totalElements: 2,
          totalPages: 2,
          last: requestedPage > 0,
        },
        insights: {
          totalRatings: 2,
          averageUserRating: 8,
          distribution: [
            { label: "0–3.9", count: 0 },
            { label: "4–5.9", count: 0 },
            { label: "6–6.9", count: 0 },
            { label: "7–7.9", count: 1 },
            { label: "8–8.9", count: 0 },
            { label: "9–10", count: 1 },
          ],
          favoriteGenres: [
            { label: "Sci fi", movieCount: 1, averageUserRating: 9 },
          ],
          favoriteDecades: [
            { label: "2000s", movieCount: 2, averageUserRating: 8 },
          ],
          averageImdbDifference: 0.5,
          definingMovies: [
            { movie: firstMovie, userRating: 9, imdbDifference: 1.4 },
          ],
        },
      }),
    });
  });

  await page.goto("/your-ratings");

  await expect(page.getByText("TASTE SNAPSHOT")).toBeVisible();
  await expect(page.getByLabel("Your rating distribution")).toBeVisible();
  await expect(
    page.getByRole("link", { name: /First Library Film/ }).first(),
  ).toBeVisible();
  await page.getByRole("button", { name: "Load more (1 of 2)" }).click();
  await expect(
    page.getByRole("link", { name: /Second Library Film/ }),
  ).toBeVisible();
});

test("keeps fresh watchlist choices compact on desktop and readable on mobile", async ({
  page,
}, testInfo) => {
  const isMobile = testInfo.project.name === "mobile-chromium";
  if (isMobile) {
    await page.setViewportSize({ width: 390, height: 844 });
  }
  await mockAuthenticatedSession(page);
  await page.route(
    "**/api/account/niklas/library/watchlist**",
    async (route) => {
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          items: {
            content: [
              {
                accountId: 7,
                movieId: 1,
                addedAt: "2024-01-01T00:00:00Z",
                movie: firstMovie,
              },
              {
                accountId: 7,
                movieId: 2,
                addedAt: "2023-01-01T00:00:00Z",
                movie: secondMovie,
              },
              {
                accountId: 7,
                movieId: 3,
                addedAt: "2022-01-01T00:00:00Z",
                movie: thirdMovie,
              },
            ],
            page: 0,
            size: 30,
            totalElements: 3,
            totalPages: 1,
            last: true,
          },
          insights: {
            totalMovies: 3,
            totalRuntimeMinutes: 300,
            averageImdbRating: 7.7,
            topGenres: [{ label: "Drama", movieCount: 2 }],
            oldestSavedAt: "2022-01-01T00:00:00Z",
            quickWatchCount: 3,
          },
        }),
      });
    },
  );
  await page.route(
    "**/api/recommendations/watchlist-tonight",
    async (route) => {
      const request = route.request().postDataJSON() as {
        excludedMovieIds?: number[];
      };
      expect(request.excludedMovieIds).toEqual([]);
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          seed: "stable-seed",
          picks: [
            {
              movie: fourthMovie,
              role: "SAFE_BET",
              explanation: "A great place to start.",
            },
            {
              movie: fifthMovie,
              role: "FORGOTTEN_GEM",
              explanation: "A well-regarded older discovery.",
            },
            {
              movie: sixthMovie,
              role: "WILD_CARD",
              explanation: "Try a different mood.",
            },
          ],
        }),
      });
    },
  );

  await page.goto("/your-watchlist");
  await page.getByRole("button", { name: "Recommend something new" }).click();

  await expect(page.getByText("Safe bet")).toBeVisible();
  await expect(page.getByText("Forgotten gem")).toBeVisible();
  await expect(page.getByText("Wild card")).toBeVisible();
  await expect(page.getByText("Try a different mood.")).toBeVisible();
  await expect(
    page.getByRole("link", { name: /New Discovery One/ }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: /First Library Film/ }),
  ).toBeVisible();
  if (isMobile) {
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth),
    ).toBe(390);
  } else {
    const recommendationBounds = await page
      .getByRole("link", { name: /New Discovery One/ })
      .boundingBox();
    const thirdRecommendationBounds = await page
      .getByRole("link", { name: /New Discovery Three/ })
      .boundingBox();
    const choicesBounds = await page
      .getByTestId("watchlist-tonight-choices")
      .boundingBox();
    expect(recommendationBounds?.width ?? 0).toBeLessThanOrEqual(184);
    const choicesCenter =
      (choicesBounds?.x ?? 0) + (choicesBounds?.width ?? 0) / 2;
    const recommendationsCenter =
      ((recommendationBounds?.x ?? 0) +
        (thirdRecommendationBounds?.x ?? 0) +
        (thirdRecommendationBounds?.width ?? 0)) /
      2;
    expect(Math.abs(recommendationsCenter - choicesCenter)).toBeLessThanOrEqual(
      1,
    );
  }
});
