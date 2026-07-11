import { expect, type Page, test } from "@playwright/test";

const transparentPixel =
  "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAqf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/ASP/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/ASP/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAY/Aqf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/ISP/2gAMAwEAAgADAAAAEP/EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QH//EFBQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QH//EFBABAQAAAAAAAAAAAAAAAAAAABD/2gAIAQEAAT8QH//Z";

const shawshank = {
  id: 1,
  imdbId: "tt0111161",
  tmdbId: 278,
  movieType: "MOVIE",
  primaryTitle: "The Shawshank Redemption",
  originalTitle: "The Shawshank Redemption",
  adult: false,
  startYear: 1994,
  runtimeMinutes: 142,
  movieGenre: ["DRAMA"],
  imdbRating: 9.3,
  imdbRatingCount: 3189003,
  description:
    "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.",
  posterImageToken: "shawshank-poster",
  backdropImageToken: "shawshank-backdrop",
  trailerYoutubeKey: "abcDEF123_-",
  rating: 8.8,
  ratingCount: 1240,
};

const partialMovie = {
  id: 2,
  primaryTitle:
    "A Very Long Movie Title That Still Needs To Fit On Small Screens",
  description: "No media is available for this catalog entry.",
};

const mockMovie = async (page: Page, movie = shawshank) => {
  await page.route(`**/api/movie/${movie.id}`, async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(movie),
    });
  });
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
        id: 7,
        username: "niklas",
        email: "niklas@example.com",
        roles: ["ROLE_USER"],
      }),
    });
  });
};

const mockMovieMedia = async (page: Page) => {
  await page.route("**/imdb-clone/movies/**", async (route) => {
    await route.fulfill({
      contentType: "image/jpeg",
      body: Buffer.from(transparentPixel, "base64"),
    });
  });
};

test("uses a broad backdrop-led desktop composition", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await mockAnonymousSession(page);
  await mockMovie(page);
  await mockMovieMedia(page);

  await page.goto("/movie?id=1");
  const hero = page.getByTestId("movie-detail-hero");
  const heroBackdrop = hero.getByTestId("movie-backdrop").first();
  await expect(hero).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "The Shawshank Redemption" }),
  ).toBeVisible();
  await expect(heroBackdrop).toHaveAttribute("data-has-image", "true");
  const heroBounds = await hero.boundingBox();
  const backdropBounds = await heroBackdrop.boundingBox();
  const posterBounds = await page
    .getByAltText("The Shawshank Redemption poster")
    .boundingBox();
  const actionsBounds = await page
    .getByTestId("movie-detail-actions")
    .boundingBox();
  const identityBounds = await page
    .getByTestId("movie-detail-identity")
    .boundingBox();
  const ratingsBounds = await page
    .getByTestId("movie-detail-ratings")
    .boundingBox();
  const watchlistButtonBounds = await page
    .getByRole("button", { name: "Add to watchlist" })
    .boundingBox();
  const rateButtonBounds = await page
    .getByRole("button", { name: "Rate movie" })
    .boundingBox();
  const shareButtonBounds = await page
    .getByRole("button", { name: "Share movie" })
    .boundingBox();
  const synopsisBounds = await page.getByTestId("movie-synopsis").boundingBox();
  const trailerBounds = await page.getByTestId("movie-trailer").boundingBox();
  const synopsisTextBounds = await page
    .getByText(shawshank.description)
    .boundingBox();
  const identityActionsGap =
    (actionsBounds?.y ?? 0) -
    ((identityBounds?.y ?? 0) + (identityBounds?.height ?? 0));
  const actionsRatingsGap =
    (ratingsBounds?.y ?? 0) -
    ((actionsBounds?.y ?? 0) + (actionsBounds?.height ?? 0));
  expect(heroBounds?.width ?? 0).toBeGreaterThan(1100);
  expect(backdropBounds?.height ?? 0).toBeGreaterThanOrEqual(425);
  expect(posterBounds?.width ?? 0).toBeGreaterThanOrEqual(215);
  expect(trailerBounds?.width ?? 0).toBeGreaterThanOrEqual(700);
  expect(trailerBounds?.width ?? 0).toBeLessThanOrEqual(720);
  expect(
    Math.abs(
      (trailerBounds?.x ?? 0) +
        (trailerBounds?.width ?? 0) / 2 -
        ((heroBounds?.x ?? 0) + (heroBounds?.width ?? 0) / 2),
    ),
  ).toBeLessThanOrEqual(1);
  expect(ratingsBounds?.y ?? 0).toBeGreaterThan(actionsBounds?.y ?? 0);
  expect(identityActionsGap).toBeGreaterThanOrEqual(0);
  expect(identityActionsGap).toBeLessThanOrEqual(12);
  expect(actionsRatingsGap).toBeGreaterThanOrEqual(0);
  expect(actionsRatingsGap).toBeLessThanOrEqual(12);
  expect(watchlistButtonBounds?.height ?? 0).toBe(40);
  expect(rateButtonBounds?.height ?? 0).toBe(40);
  expect(shareButtonBounds?.height ?? 0).toBe(40);
  expect(
    Math.abs(
      (posterBounds?.y ?? 0) +
        (posterBounds?.height ?? 0) -
        ((ratingsBounds?.y ?? 0) + (ratingsBounds?.height ?? 0)),
    ),
  ).toBeLessThanOrEqual(1);
  expect(
    (trailerBounds?.y ?? 0) -
      ((posterBounds?.y ?? 0) + (posterBounds?.height ?? 0)),
  ).toBeGreaterThanOrEqual(20);
  expect(
    Math.abs((synopsisBounds?.width ?? 0) - (heroBounds?.width ?? 0)),
  ).toBeLessThanOrEqual(1);
  expect(
    Math.abs((synopsisTextBounds?.width ?? 0) - (synopsisBounds?.width ?? 0)),
  ).toBeLessThanOrEqual(1);
  expect(
    (synopsisBounds?.y ?? 0) -
      ((trailerBounds?.y ?? 0) + (trailerBounds?.height ?? 0)),
  ).toBeLessThanOrEqual(24);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    1440,
  );

  await page.setViewportSize({ width: 1366, height: 768 });
  await expect(hero).toBeVisible();
  expect((await hero.boundingBox())?.width ?? 0).toBeGreaterThan(1050);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    1366,
  );

  await page.setViewportSize({ width: 960, height: 1000 });
  const mediumPosterBounds = await page
    .getByAltText("The Shawshank Redemption poster")
    .boundingBox();
  const mediumRatingsBounds = await page
    .getByTestId("movie-detail-ratings")
    .boundingBox();
  const mediumTrailerBounds = await page
    .getByTestId("movie-trailer")
    .boundingBox();
  expect(
    Math.abs(
      (mediumPosterBounds?.y ?? 0) +
        (mediumPosterBounds?.height ?? 0) -
        ((mediumRatingsBounds?.y ?? 0) + (mediumRatingsBounds?.height ?? 0)),
    ),
  ).toBeLessThanOrEqual(1);
  expect(mediumTrailerBounds?.width ?? 0).toBeLessThanOrEqual(720);
  expect(mediumTrailerBounds?.y ?? 0).toBeGreaterThan(
    (mediumPosterBounds?.y ?? 0) + (mediumPosterBounds?.height ?? 0),
  );
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    960,
  );
});

test("keeps poster and identity connected without mobile overflow", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockAnonymousSession(page);
  await mockMovie(page);
  await mockMovieMedia(page);

  await page.goto("/movie?id=1");
  const posterBounds = await page
    .getByAltText("The Shawshank Redemption poster")
    .boundingBox();
  const titleBounds = await page
    .getByRole("heading", { name: "The Shawshank Redemption" })
    .boundingBox();
  const ratings = page.getByText("IMDb rating").locator("..").locator("..");
  const actionsBounds = await page
    .getByTestId("movie-detail-actions")
    .boundingBox();
  const ratingsBounds = await page
    .getByTestId("movie-detail-ratings")
    .boundingBox();

  expect(titleBounds?.x ?? 0).toBeGreaterThan(posterBounds?.x ?? 0);
  expect(titleBounds?.y ?? 0).toBeLessThan(
    (posterBounds?.y ?? 0) + (posterBounds?.height ?? 0),
  );
  await expect(ratings).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Add to watchlist" }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "Rate movie" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Share movie" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Play trailer" }),
  ).toBeVisible();
  expect(actionsBounds?.y ?? 0).toBeLessThan(ratingsBounds?.y ?? 0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    390,
  );
});

test("renders deliberate fallbacks for partial movie data", async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 700 });
  await mockAnonymousSession(page);
  await mockMovie(page, partialMovie);

  await page.goto("/movie?id=2");

  await expect(page.getByTestId("movie-backdrop")).toHaveAttribute(
    "data-has-image",
    "false",
  );
  await expect(
    page.getByAltText(`${partialMovie.primaryTitle} poster`),
  ).toBeVisible();
  await expect(
    page.getByText("No media is available for this catalog entry."),
  ).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    320,
  );
});

test("provides sign-in guidance and share feedback", async ({ page }) => {
  await mockAnonymousSession(page);
  await mockMovie(page);
  await mockMovieMedia(page);
  await page.addInitScript(() => {
    Object.defineProperty(navigator, "share", {
      configurable: true,
      value: async () => undefined,
    });
  });

  await page.goto("/movie?id=1");
  await page.getByRole("button", { name: "Rate movie" }).click();
  await expect(page.getByText("Sign in to rate this movie.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();

  await page.getByRole("button", { name: "Share movie" }).click();
  await expect(page.getByText("Movie shared.")).toBeVisible();
});

test("loads the privacy-enhanced trailer only after user interaction", async ({
  page,
}) => {
  await mockAnonymousSession(page);
  await mockMovie(page);
  await mockMovieMedia(page);
  await page.route("https://www.youtube-nocookie.com/**", async (route) => {
    await route.fulfill({ contentType: "text/html", body: "<html></html>" });
  });

  await page.goto("/movie?id=1");
  await expect(
    page.locator('iframe[title="The Shawshank Redemption trailer"]'),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Play trailer" }).click();

  const iframe = page.locator(
    'iframe[title="The Shawshank Redemption trailer"]',
  );
  await expect(iframe).toHaveAttribute(
    "src",
    "https://www.youtube-nocookie.com/embed/abcDEF123_-?autoplay=1&playsinline=1&rel=0",
  );
});

test("saves a rating in the authenticated dialog flow", async ({ page }) => {
  let savedRating = 6;
  await mockAuthenticatedSession(page);
  await mockMovie(page);
  await mockMovieMedia(page);
  await page.route("**/api/account/niklas/watchlist**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ content: [], last: true }),
    });
  });
  await page.route("**/api/account/niklas/ratings**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [{ movieId: 1, rating: savedRating }],
        last: true,
      }),
    });
  });
  await page.route("**/api/movie-rating/1/rating-score/*", async (route) => {
    savedRating = Number(
      new URL(route.request().url()).pathname.split("/").at(-1),
    );
    await route.fulfill({ status: 200, body: "" });
  });

  await page.goto("/movie?id=1");
  await page.getByRole("button", { name: "Your rating: 6" }).click();
  const dialog = page.getByRole("dialog", { name: "Rate this movie" });
  await expect(dialog).toBeVisible();
  await dialog.locator("label").filter({ hasText: "9 out of 10" }).click();
  await dialog.getByRole("button", { name: "Save rating" }).click();

  await expect(dialog).toBeHidden();
  await expect(page.getByText("Rating saved.")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Your rating: 9" }),
  ).toBeVisible();
});

test("keyboard navigation reaches movie actions", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockAnonymousSession(page);
  await mockMovie(page);
  await mockMovieMedia(page);
  await page.goto("/movie?id=1");
  await expect(
    page.getByRole("heading", { name: "The Shawshank Redemption" }),
  ).toBeVisible();

  const focusOrder = [
    page.getByRole("link", { name: "IMDb Clone" }),
    page.getByRole("textbox", { name: "search movies" }),
    page.getByRole("link", { name: "Sign in" }),
    page.getByRole("button", { name: "Add to watchlist" }),
    page.getByRole("button", { name: "Rate movie" }),
    page.getByRole("button", { name: "Share movie" }),
    page.getByRole("button", { name: "Play trailer" }),
  ];

  for (const target of focusOrder) {
    await page.keyboard.press("Tab");
    await expect(target).toBeFocused();
  }
});
