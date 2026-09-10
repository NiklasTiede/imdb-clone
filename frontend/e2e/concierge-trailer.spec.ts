import { expect, test } from "@playwright/test";
import path from "node:path";

test.use({
  launchOptions: {
    args: [
      "--use-fake-ui-for-media-stream",
      "--use-fake-device-for-media-stream",
      `--use-file-for-fake-audio-capture=${path.resolve("../agent/evals/voice/forrest-gump-en.wav")}`,
    ],
  },
  permissions: ["microphone"],
});

test("voice centers trailers across routes and repeated requests without starting playback", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401 }),
  );
  const movies = [
    { id: 6, primaryTitle: "Forrest Gump", trailerYoutubeKey: "abcDEF123_-" },
    { id: 7, primaryTitle: "Arrival", trailerYoutubeKey: "xyzABC123_-" },
    { id: 8, primaryTitle: "Movie without trailer", trailerYoutubeKey: null },
    { id: 9, primaryTitle: "Another movie", trailerYoutubeKey: null },
  ];
  for (const movie of movies) {
    await page.route(`**/api/v1/movies/${movie.id}`, async (route) => {
      // Exercise scrolling after the destination's asynchronous load.
      await new Promise((resolve) => setTimeout(resolve, 100));
      await route.fulfill({
        json: {
          ...movie,
          movieType: "MOVIE",
          startYear: 1994,
          description: "A movie from the local catalog.",
          movieGenre: ["DRAMA"],
        },
      });
    });
    await page.route(`**/api/v1/movies/${movie.id}/comments**`, (route) =>
      route.fulfill({
        json: {
          content: [],
          page: 0,
          size: 10,
          totalElements: 0,
          totalPages: 0,
          last: true,
        },
      }),
    );
  }
  await page.route("**/api/v1/recommendations/**", (route) =>
    route.fulfill({ json: { items: [] } }),
  );
  const youtubeRequests: string[] = [];
  await page.route(
    /https:\/\/(?:www\.)?youtube(?:-nocookie)?\.com\//,
    (route) => {
      youtubeRequests.push(route.request().url());
      return route.fulfill({
        contentType: "text/html",
        body: "<html><body>Fixture player</body></html>",
      });
    },
  );
  let emit: ((movieId: number, trailer?: boolean) => void) | undefined;
  let connections = 0;
  let ended = false;
  await page.routeWebSocket("**/v1/voice", (socket) => {
    connections++;
    let turn = 0;
    socket.onMessage((message) => {
      if (typeof message !== "string") return;
      const frame = JSON.parse(message) as { type: string };
      if (frame.type === "start")
        socket.send(JSON.stringify({ type: "ready" }));
      if (frame.type === "end") ended = true;
    });
    emit = (movieId, trailer = true) => {
      turn++;
      socket.send(JSON.stringify({ type: "interrupt", turn }));
      const movie = movies.find((candidate) => candidate.id === movieId);
      socket.send(
        JSON.stringify({
          type: "movie-card",
          turn,
          movie: {
            movieId,
            primaryTitle: movie?.primaryTitle,
            movieType: "MOVIE",
            genres: [],
          },
        }),
      );
      socket.send(
        JSON.stringify({
          type: "ui-action",
          turn,
          action: {
            type: trailer ? "open_movie_trailer" : "open_movie",
            movieId,
          },
        }),
      );
    };
  });
  await page.goto("/movie-search");
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  await page.getByRole("button", { name: "Start voice" }).click();
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  for (const movieId of [6, 6, 7, 8]) {
    await page.evaluate(() => window.scrollTo(0, 0));
    emit?.(movieId);
    await expect(page).toHaveURL(`/movie?id=${movieId}#trailer`);
    const target = page.getByRole("region", { name: "Movie trailer section" });
    await expect(target).toBeFocused();
    await expect
      .poll(async () => {
        const bounds = await target.boundingBox();
        const height = page.viewportSize()?.height ?? 0;
        return bounds
          ? Math.abs(bounds.y + bounds.height / 2 - height / 2)
          : Infinity;
      })
      .toBeLessThan(3);
    if (movieId === 8) {
      await expect(
        page.getByText(
          "No trailer is available for Movie without trailer in our catalog.",
        ),
      ).toBeVisible();
    } else {
      await expect(
        page.getByRole("button", { name: "Play trailer" }),
      ).toBeVisible();
    }
    await expect(
      page.getByRole("button", { name: "End voice session" }).last(),
    ).toBeVisible();
  }
  expect(youtubeRequests).toEqual([]);
  expect(connections).toBe(1);
  expect(ended).toBe(false);
  emit?.(6, false);
  await expect(page).toHaveURL("/movie?id=6");
  await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(0);
  // Normal opens reset both cached/repeated routes and a fresh asynchronous load.
  for (const movieId of [6, 7, 9]) {
    await page
      .getByRole("region", { name: "Movie trailer section" })
      .scrollIntoViewIfNeeded();
    await expect
      .poll(() => page.evaluate(() => window.scrollY))
      .toBeGreaterThan(0);
    emit?.(movieId, false);
    await expect(page).toHaveURL(`/movie?id=${movieId}`);
    await expect(
      page.getByRole("heading", {
        name: movies.find((movie) => movie.id === movieId)!.primaryTitle,
        exact: true,
      }),
    ).toBeVisible();
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(0);
  }
  emit?.(6);
  await expect(page).toHaveURL("/movie?id=6#trailer");
  await expect(
    page.getByRole("button", { name: "Play trailer" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Play trailer" }).click();
  await expect(page.getByTitle("Forrest Gump trailer")).toHaveAttribute(
    "src",
    /youtube-nocookie\.com\/embed\/abcDEF123_-\?autoplay=1/,
  );
  // Re-focusing an already mounted player must not reload/restart its video.
  emit?.(6);
  await expect(
    page.getByRole("region", { name: "Movie trailer section" }),
  ).toBeFocused();
  expect(youtubeRequests).toHaveLength(1);
  emit?.(7);
  await expect(page).toHaveURL("/movie?id=7#trailer");
  await expect(
    page.getByRole("button", { name: "Play trailer" }),
  ).toBeVisible();
  await expect(page.getByTitle("Arrival trailer")).toHaveCount(0);
  expect(youtubeRequests).toHaveLength(1);
  await page.getByRole("button", { name: "End voice session" }).last().click();
  await expect.poll(() => ended).toBe(true);
});
