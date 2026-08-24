import { expect, type Page, test } from "@playwright/test";

const conversationId = "1234567890abcdef1234567890abcdef";

const sse = (type: string, sequence: number, payload: object): string =>
  `event: ${type}\nid: ${sequence}\ndata: ${JSON.stringify({ type, sequence, ...payload })}\n\n`;

const mockAnonymousShell = async (page: Page) => {
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({ status: 401, body: "" });
  });
  await page.route("**/api/search/movies**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
      }),
    });
  });
};

test("public concierge streams a grounded movie into its responsive drawer", async ({
  page,
}) => {
  await mockAnonymousShell(page);
  const clientIds: string[] = [];
  await page.route("**/concierge-api/v1/conversations", async (route) => {
    clientIds.push(route.request().headers()["x-concierge-client-id"] ?? "");
    await route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({ conversationId }),
    });
  });
  await page.route(
    `**/concierge-api/v1/conversations/${conversationId}/messages`,
    async (route) => {
      clientIds.push(route.request().headers()["x-concierge-client-id"] ?? "");
      expect(route.request().postDataJSON()).toEqual({
        message: "Find a thoughtful science-fiction movie",
      });
      await route.fulfill({
        status: 200,
        contentType: "text/event-stream",
        body:
          sse("status", 1, { status: "searching_catalog" }) +
          sse("movie-card", 2, {
            movie: {
              movieId: 42,
              primaryTitle: "Arrival",
              originalTitle: "Arrival",
              movieType: "MOVIE",
              startYear: 2016,
              runtimeMinutes: 116,
              genres: ["DRAMA", "SCI_FI"],
              imdbRating: 7.9,
              imdbRatingCount: 800000,
              description: "A linguist meets visitors from another world.",
              posterImageToken: null,
              explanation: "A thoughtful science-fiction drama.",
            },
          }) +
          sse("text", 3, {
            delta: "Arrival is a grounded match from this catalog.",
          }) +
          sse("usage", 4, {
            usage: {
              model: "deterministic-e2e",
              requests: 2,
              toolCalls: 1,
              inputTokens: 100,
              outputTokens: 20,
              totalTokens: 120,
              estimatedCostUsd: "0.001",
              costAvailable: true,
            },
          }) +
          sse("completion", 5, { conversationId, outcome: "success" }),
      });
    },
  );

  await page.goto("/movie-search");
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();

  const drawer = page.getByRole("dialog", { name: "Movie Concierge" });
  await expect(drawer).toBeVisible();
  const bounds = await drawer.boundingBox();
  const viewport = page.viewportSize();
  expect(bounds).not.toBeNull();
  expect(viewport).not.toBeNull();
  if ((viewport?.width ?? 0) < 600) {
    expect(
      Math.abs((bounds?.width ?? 0) - (viewport?.width ?? 0)),
    ).toBeLessThanOrEqual(1);
  } else {
    expect(Math.abs((bounds?.width ?? 0) - 440)).toBeLessThanOrEqual(1);
  }

  const input = page.getByRole("textbox", { name: "Ask the Movie Concierge" });
  await expect(input).toHaveCSS("color", "rgba(255, 255, 255, 0.92)");
  expect(
    await input.evaluate((element) =>
      getComputedStyle(element, "::placeholder").getPropertyValue("color"),
    ),
  ).toBe("rgba(255, 255, 255, 0.62)");

  await input.fill("Find a thoughtful science-fiction movie");
  await page.getByRole("button", { name: "Send concierge message" }).click();

  await expect(
    page.getByText("Arrival is a grounded match from this catalog."),
  ).toBeVisible();
  await expect(page.getByTestId("concierge-movie-card")).toBeVisible();
  const movieCard = page.getByTestId("concierge-movie-card");
  await expect(movieCard.getByText("2016")).toHaveCSS(
    "color",
    "rgba(255, 255, 255, 0.78)",
  );
  await expect(movieCard.getByText("116 min")).toHaveCSS(
    "color",
    "rgba(255, 255, 255, 0.78)",
  );
  await expect(movieCard.getByText("7.9")).toHaveCSS(
    "color",
    "rgba(255, 255, 255, 0.92)",
  );
  await expect(
    movieCard.getByRole("link", { name: "Arrival" }),
  ).toHaveAttribute("href", "/movie?id=42");
  expect(clientIds).toHaveLength(2);
  expect(clientIds[0]).toMatch(/^browser-[a-f0-9-]{36}:anonymous$/);
  expect(clientIds[1]).toBe(clientIds[0]);

  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await expect(drawer).toBeHidden();
});

test("grounded concierge action opens the movie page without leaving an overlay", async ({
  page,
}) => {
  await mockAnonymousShell(page);
  await page.route("**/api/movie/42", async (route) => {
    await route.fulfill({ status: 404, body: "" });
  });
  await page.route("**/concierge-api/v1/conversations", async (route) => {
    await route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({ conversationId }),
    });
  });
  await page.route(
    `**/concierge-api/v1/conversations/${conversationId}/messages`,
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "text/event-stream",
        body:
          sse("movie-card", 1, {
            movie: {
              movieId: 42,
              primaryTitle: "Arrival",
              originalTitle: "Arrival",
              movieType: "MOVIE",
              startYear: 2016,
              runtimeMinutes: 116,
              genres: ["DRAMA", "SCI_FI"],
            },
          }) +
          sse("text", 2, { delta: "Opening Arrival." }) +
          sse("ui-action", 3, {
            action: { type: "open_movie", movieId: 42 },
          }) +
          sse("completion", 4, { conversationId, outcome: "success" }),
      });
    },
  );

  await page.goto("/movie-search");
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  const drawer = page.getByRole("dialog", { name: "Movie Concierge" });
  await page
    .getByRole("textbox", { name: "Ask the Movie Concierge" })
    .fill("Open Arrival");
  await page.getByRole("button", { name: "Send concierge message" }).click();

  await expect(page).toHaveURL(/\/movie\?id=42$/);
  await expect(drawer).toBeHidden();
});
