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

const commentAuthors = [
  {
    id: 7,
    username: "niklas",
    displayName: "Niklas Tiede",
    imageUrlToken: "niklas-avatar",
  },
];

type MovieFixture = {
  id: number;
  [property: string]: unknown;
};

const mockMovie = async (
  page: Page,
  movie: MovieFixture = shawshank,
  comments: Array<Record<string, unknown>> = [],
) => {
  await page.route(`**/api/movie/${movie.id}`, async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(movie),
    });
  });
  await page.route(`**/api/comment/${movie.id}/comments**`, async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: comments,
        last: true,
        page: 0,
        size: 10,
        totalElements: comments.length,
        totalPages: comments.length > 0 ? 1 : 0,
      }),
    });
  });
  await page.route("**/api/account/summaries**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(commentAuthors),
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
  await page.route("**/api/account/me/profile", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ username: "niklas", email: "niklas@example.com" }),
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
  const commentsBounds = await page.getByTestId("movie-comments").boundingBox();
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
    (trailerBounds?.y ?? 0) -
      ((synopsisBounds?.y ?? 0) + (synopsisBounds?.height ?? 0)),
  ).toBeGreaterThan(0);
  expect(commentsBounds?.y ?? 0).toBeGreaterThan(
    (trailerBounds?.y ?? 0) + (trailerBounds?.height ?? 0),
  );
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
  const mediumSynopsisBounds = await page
    .getByTestId("movie-synopsis")
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
    (mediumSynopsisBounds?.y ?? 0) + (mediumSynopsisBounds?.height ?? 0),
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
  await expect(
    page.getByRole("button", { name: "Sign in", exact: true }),
  ).toBeVisible();

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

test("publishes, edits, and deletes an owned movie comment", async ({
  page,
}) => {
  const comments: Array<Record<string, unknown>> = [
    {
      id: 11,
      message: "An unforgettable ending.",
      accountId: 7,
      movieId: 1,
      createdAtInUtc: "2026-07-11T10:00:00Z",
      modifiedAtInUtc: "2026-07-11T10:00:00Z",
    },
  ];
  await mockAuthenticatedSession(page);
  await mockMovie(page, shawshank, comments);
  await mockMovieMedia(page);
  await page.route("**/api/comment/*", async (route) => {
    const request = route.request();
    const pathname = new URL(request.url()).pathname;
    const commentId = Number(pathname.split("/").at(-1));
    const payload =
      request.method() === "POST" || request.method() === "PUT"
        ? (request.postDataJSON() as { message?: string })
        : null;

    if (request.method() === "POST" && commentId === 1) {
      const created = {
        id: 12,
        message: payload?.message,
        accountId: 7,
        movieId: 1,
        createdAtInUtc: "2026-07-11T12:00:00Z",
        modifiedAtInUtc: "2026-07-11T12:00:00Z",
      };
      comments.unshift(created);
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify(created),
        status: 201,
      });
      return;
    }

    if (request.method() === "PUT") {
      const existing = comments.find((comment) => comment.id === commentId);
      if (existing) {
        existing.message = payload?.message;
        existing.modifiedAtInUtc = "2026-07-11T12:10:00Z";
      }
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify(existing),
      });
      return;
    }

    if (request.method() === "DELETE") {
      const index = comments.findIndex((comment) => comment.id === commentId);
      if (index >= 0) {
        comments.splice(index, 1);
      }
      await route.fulfill({ status: 204 });
      return;
    }

    await route.fallback();
  });

  await page.goto("/movie?id=1");
  await expect(page.getByRole("heading", { name: "Community" })).toBeVisible();
  await expect(page.getByText("An unforgettable ending.")).toBeVisible();

  await page
    .getByRole("textbox", { name: "Comment on The Shawshank Redemption" })
    .fill("Hope is a good thing.");
  await page.getByRole("button", { name: "Publish comment" }).click();
  const createdComment = page.getByTestId("comment-12");
  await expect(createdComment.getByText("Hope is a good thing.")).toBeVisible();

  await createdComment
    .getByRole("button", { name: "Manage comment by Niklas Tiede" })
    .click();
  await page.getByRole("menuitem", { name: "Edit" }).click();
  const editor = createdComment.getByRole("textbox", { name: "Edit comment" });
  await editor.fill("Hope is the best of things.");
  await createdComment.getByRole("button", { name: "Save" }).click();
  await expect(
    createdComment.getByText("Hope is the best of things."),
  ).toBeVisible();

  await createdComment
    .getByRole("button", { name: "Manage comment by Niklas Tiede" })
    .click();
  await page.getByRole("menuitem", { name: "Delete" }).click();
  const deleteDialog = page.getByRole("dialog", { name: "Delete comment?" });
  await deleteDialog.getByRole("button", { name: "Delete" }).click();
  await expect(createdComment).toHaveCount(0);
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
  const nineStarRating = dialog.getByRole("radio", { name: "9 out of 10" });
  await nineStarRating.focus();
  await page.keyboard.press("Space");
  await expect(nineStarRating).toBeChecked();
  await dialog.getByRole("button", { name: "Save rating" }).click();

  await expect(dialog).toBeHidden();
  await expect(page.getByText("Rating saved.")).toBeVisible();
  expect(savedRating).toBe(9);
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
