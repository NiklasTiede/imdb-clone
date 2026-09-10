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

for (const signedIn of [false, true]) {
  test(`${signedIn ? "signed-in" : "guest"} voice navigation preserves the session and visible search criteria`, async ({
    page,
  }) => {
    const user = {
      id: 7,
      username: "voice_fixture",
      email: "voice@example.invalid",
      roles: ["ROLE_USER"],
    };
    for (const url of ["**/api/v1/auth/me", "**/api/v1/accounts/me/profile"]) {
      await page.route(url, (route) =>
        route.fulfill(signedIn ? { json: user } : { status: 401, body: "" }),
      );
    }
    await page.route("**/api/v1/accounts/me/passkeys", (route) =>
      route.fulfill({ json: [] }),
    );
    await page.route("**/api/v1/auth/concierge-delegation", (route) =>
      route.fulfill({
        json: {
          token: "synthetic-delegation",
          expiresAt: "2026-09-09T15:00:00Z",
        },
      }),
    );
    const mutations: string[] = [];
    page.on("request", (request) => {
      if (
        request.url().includes("/api/v1/accounts/") &&
        request.method() !== "GET"
      )
        mutations.push(request.url());
    });
    for (const kind of ["ratings", "watchlist"]) {
      await page.route(`**/api/v1/accounts/**/library/${kind}**`, (route) =>
        route.fulfill({
          json: {
            items: {
              content: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
              last: true,
            },
            insights: {},
          },
        }),
      );
    }
    const searches: {
      query: string | null;
      page: string | null;
      filters: unknown;
    }[] = [];
    await page.route("**/api/v1/search/movies**", (route) => {
      const params = new URL(route.request().url()).searchParams;
      searches.push({
        query: params.get("query"),
        page: params.get("page"),
        filters: route.request().postDataJSON() as unknown,
      });
      return route.fulfill({
        json: {
          content: [
            {
              id: 6,
              primaryTitle: "Forrest Gump",
              movieType: "MOVIE",
              movieGenre: ["DRAMA"],
              startYear: 1994,
              runtimeMinutes: 142,
            },
          ],
          page: Number(params.get("page")),
          size: 24,
          totalElements: 48,
          totalPages: 2,
          last: false,
        },
      });
    });
    let emit: ((action: object) => void) | undefined;
    const pageContexts: object[] = [];
    let connections = 0;
    let ended = false;
    await page.routeWebSocket("**/v1/voice", (socket) => {
      connections++;
      let turn = 0;
      socket.onMessage((message) => {
        if (typeof message !== "string") return;
        const frame = JSON.parse(message) as { type: string; context?: object };
        if (frame.type === "context" && frame.context)
          pageContexts.push(frame.context);
        if (frame.type === "start")
          socket.send(JSON.stringify({ type: "ready" }));
        if (frame.type === "end") ended = true;
      });
      emit = (action) => {
        turn++;
        socket.send(JSON.stringify({ type: "interrupt", turn }));
        socket.send(JSON.stringify({ type: "ui-action", turn, action }));
      };
    });
    await page.goto("/movie-search");
    await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
    await page.getByText("Data sources & credits", { exact: true }).click();
    await expect(
      page.getByText(
        "This product uses the TMDB API but is not endorsed or certified by TMDB.",
      ),
    ).toBeVisible();
    const logo = page.getByRole("img", { name: "The Movie Database (TMDB)" });
    await expect(logo).toBeVisible();
    await expect
      .poll(() =>
        logo.evaluate((image: HTMLImageElement) => image.naturalWidth),
      )
      .toBeGreaterThan(0);
    if (!signedIn)
      await page.screenshot({
        path: `/tmp/tmdb-credits-${test.info().project.name}.png`,
      });
    await page.getByText("Data sources & credits", { exact: true }).click();
    const country = page.getByRole("combobox", { name: "Streaming in" });
    await expect(country).toHaveValue("CH");
    await page.getByRole("button", { name: "Start voice" }).click();
    await expect(page.getByRole("status")).toHaveText("Listening to you");
    await expect
      .poll(() => pageContexts.at(-1))
      .toEqual({
        page: "search",
        searchQuery: "",
        streamingCountry: "CH",
      });
    await country.selectOption("DE");
    await expect
      .poll(() => pageContexts.at(-1))
      .toEqual({
        page: "search",
        searchQuery: "",
        streamingCountry: "DE",
      });
    for (const [destination, route] of [
      ["settings", "/account-settings"],
      ["ratings", "/your-ratings"],
      ["watchlist", "/your-watchlist"],
      ["home", "/"],
    ]) {
      emit?.({ type: "open_page", destination });
      await expect(page).toHaveURL(
        new RegExp(`${signedIn || destination === "home" ? route : "/login"}$`),
      );
      await expect(
        page.getByRole("button", { name: "End voice session" }).last(),
      ).toBeVisible();
      await expect
        .poll(() => pageContexts.at(-1))
        .toEqual({
          page: signedIn || destination === "home" ? destination : "login",
          streamingCountry: "DE",
        });
    }
    emit?.({
      type: "show_search_results",
      query: "Forrest Gump",
      genres: ["DRAMA", "ROMANCE"],
      movieType: "MOVIE",
      minStartYear: 1990,
      maxStartYear: 1999,
      maxRuntimeMinutes: 150,
    });
    await expect(
      page.getByRole("textbox", { name: "search movies" }),
    ).toHaveValue("Forrest Gump");
    await expect(
      page.getByRole("heading", { name: 'Results for "Forrest Gump"' }),
    ).toBeVisible();
    await expect
      .poll(() => searches.at(-1))
      .toEqual({
        query: "Forrest Gump",
        page: "0",
        filters: {
          movieGenre: ["DRAMA", "ROMANCE"],
          movieType: "MOVIE",
          minStartYear: 1990,
          maxStartYear: 1999,
          maxRuntimeMinutes: 150,
        },
      });
    await page
      .getByRole("button", { name: "Go to page 2", exact: true })
      .click();
    await expect.poll(() => searches.at(-1)?.page).toBe("1");
    expect(new URL(page.url()).searchParams.getAll("genre")).toEqual([
      "DRAMA",
      "ROMANCE",
    ]);
    emit?.({ type: "show_search_results", query: "Arrival" });
    await expect(
      page.getByRole("textbox", { name: "search movies" }),
    ).toHaveValue("Arrival");
    await expect
      .poll(() => searches.at(-1))
      .toEqual({ query: "Arrival", page: "0", filters: {} });
    expect(new URL(page.url()).searchParams.has("page")).toBe(false);
    expect(new URL(page.url()).searchParams.has("genre")).toBe(false);
    // A no-op manual submission must not cause a later voice request to be
    // overwritten by the search input's 300 ms debounce.
    const searchInput = page.getByRole("textbox", { name: "search movies" });
    await searchInput.press("Enter");
    emit?.({ type: "show_search_results", query: "Forrest Gump" });
    await expect(searchInput).toHaveValue("Forrest Gump");
    emit?.({ type: "show_search_results", query: "Arrival" });
    await page.waitForTimeout(500);
    await expect(searchInput).toHaveValue("Arrival");
    expect(new URL(page.url()).searchParams.get("query")).toBe("Arrival");
    expect(mutations).toEqual([]);
    expect(connections).toBe(1);
    await expect
      .poll(() => pageContexts.at(-1))
      .toEqual({
        page: "search",
        searchQuery: "Arrival",
        streamingCountry: "DE",
      });
    expect(pageContexts).toContainEqual({
      page: "home",
      streamingCountry: "DE",
    });
    if (signedIn)
      expect(pageContexts).toContainEqual({
        page: "ratings",
        streamingCountry: "DE",
      });
    expect(ended).toBe(false);
    await expect(
      page.getByRole("button", { name: "Undo", exact: true }),
    ).toBeHidden();
    await page
      .getByRole("button", { name: "End voice session" })
      .last()
      .click();
    await expect.poll(() => ended).toBe(true);
    await page.reload();
    await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
    await expect(
      page.getByRole("combobox", { name: "Streaming in" }),
    ).toHaveValue("DE");
  });
}
