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

test("real browser audio capture, playback, navigation and ending voice", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  await page.route("**/api/v1/search/movies**", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
      }),
    }),
  );
  let bytes = 0;
  let ended = false;
  let mute = false;
  let sendAction: (() => void) | undefined;
  await page.routeWebSocket("**/v1/voice", (socket) => {
    socket.send(JSON.stringify({ type: "ready" }));
    socket.onMessage((message) => {
      if (typeof message === "string") {
        const command = JSON.parse(message) as { type: string };
        if (command.type === "end") ended = true;
        if (command.type === "mute") mute = true;
        return;
      }
      bytes += message.byteLength;
    });
    sendAction = () => {
      socket.send(JSON.stringify({ type: "interrupt", turn: 1 }));
      socket.send(
        JSON.stringify({
          type: "transcript",
          turn: 1,
          speaker: "user",
          text: "Find Forrest Gump and open it",
          final: true,
        }),
      );
      socket.send(
        JSON.stringify({
          type: "movie-card",
          turn: 1,
          movie: {
            movieId: 42,
            primaryTitle: "Forrest Gump",
            movieType: "MOVIE",
            genres: [],
            runtimeMinutes: 142,
          },
        }),
      );
      // Providers generate speech faster than real time: a normal answer can
      // arrive in a burst while the browser is still playing its first words.
      for (let chunk = 0; chunk < 45; chunk++) socket.send(Buffer.alloc(24000));
      socket.send(
        JSON.stringify({
          type: "ui-action",
          turn: 1,
          action: { type: "open_movie", movieId: 42 },
        }),
      );
    };
  });
  await page.goto("/movie-search");
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  await page.getByRole("button", { name: "Start voice" }).click();
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  await expect.poll(() => bytes).toBeGreaterThan(4800);
  await page.getByRole("button", { name: "Mute microphone" }).click();
  await expect.poll(() => mute).toBe(true);
  await page.getByRole("button", { name: "Resume microphone" }).click();
  sendAction?.();
  await expect(page).toHaveURL(/\/movie\?id=42$/);
  await expect(
    page.getByRole("button", { name: "Expand voice conversation" }).last(),
  ).toBeVisible();
  expect(ended).toBe(false);
  await page.getByRole("button", { name: "End voice session" }).last().click();
  await expect.poll(() => ended).toBe(true);
  await expect(
    page.getByRole("button", { name: "Ask the Movie Concierge" }),
  ).toBeVisible();
});

test("microphone denial shows help and preserves text fallback", async ({
  page,
}) => {
  await page.addInitScript(() => {
    navigator.mediaDevices.getUserMedia = () =>
      Promise.reject(new DOMException("Permission denied", "NotAllowedError"));
  });
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  await page.goto("/movie-search");
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  await page.getByRole("button", { name: "Start voice" }).click();
  await expect(page.getByRole("alert")).toContainText("macOS");
  await expect(
    page.getByRole("textbox", { name: "Ask the Movie Concierge" }),
  ).toBeEnabled();
});

test("reply controls survive audio delivery gaps until completion or interruption", async ({
  page,
}) => {
  await page.addInitScript(() => {
    const state = window as typeof window & { voiceEndedChunks: number };
    state.voiceEndedChunks = 0;
    const OriginalContext = window.AudioContext;
    window.AudioContext = class extends OriginalContext {
      override createBufferSource() {
        const source = super.createBufferSource();
        source.addEventListener("ended", () => state.voiceEndedChunks++);
        return source;
      }
    };
  });
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  let sendAudio: (() => void) | undefined;
  let finishReply: (() => void) | undefined;
  let interrupted = false;
  await page.routeWebSocket("**/v1/voice", (socket) => {
    socket.send(JSON.stringify({ type: "ready" }));
    socket.onMessage((message) => {
      if (typeof message === "string") {
        const command = JSON.parse(message) as { type: string };
        if (command.type === "interrupt") interrupted = true;
      }
    });
    sendAudio = () => socket.send(Buffer.alloc(4800));
    finishReply = () => socket.send(JSON.stringify({ type: "reply-complete" }));
  });
  await page.goto("/movie-search");
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  await page.getByRole("button", { name: "Start voice" }).click();
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  sendAudio?.();
  const interrupt = page.getByRole("button", {
    name: "Interrupt reply",
    exact: true,
  });
  await expect(interrupt).toBeVisible();
  // Wait for the real AudioContext to drain, leaving an intentional delivery gap.
  await expect
    .poll(() =>
      page.evaluate(() => Number(Reflect.get(window, "voiceEndedChunks"))),
    )
    .toBe(1);
  await expect(interrupt).toBeVisible();
  await expect(page.getByRole("status")).toHaveText("Concierge is speaking");
  sendAudio?.();
  finishReply?.();
  await expect
    .poll(() =>
      page.evaluate(() => Number(Reflect.get(window, "voiceEndedChunks"))),
    )
    .toBe(2);
  await expect(interrupt).toBeHidden();
  sendAudio?.();
  await expect(interrupt).toBeVisible();
  await interrupt.click();
  await expect.poll(() => interrupted).toBe(true);
  await expect(interrupt).toBeHidden();
  await page.getByRole("button", { name: "End voice session" }).click();
});

test("delegated voice opens and refreshes the watchlist without ending its session", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({
      json: {
        id: 7,
        username: "voice_fixture",
        email: "voice@example.invalid",
        roles: ["ROLE_USER"],
      },
    }),
  );
  await page.route("**/api/v1/accounts/me/profile", (route) =>
    route.fulfill({
      json: {
        id: 7,
        username: "voice_fixture",
        email: "voice@example.invalid",
        roles: ["ROLE_USER"],
      },
    }),
  );
  let grants = 0;
  await page.route("**/api/v1/auth/concierge-delegation", (route) => {
    grants++;
    return route.fulfill({
      json: {
        token: "synthetic-delegation",
        expiresAt: "2026-09-09T15:00:00Z",
      },
    });
  });
  let reads = 0;
  await page.route("**/api/v1/accounts/**/library/watchlist**", (route) => {
    reads++;
    return route.fulfill({
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
    });
  });
  let started = false;
  let ended = false;
  let saved: (() => void) | undefined;
  await page.routeWebSocket("**/v1/voice", (socket) => {
    socket.onMessage((message) => {
      if (typeof message !== "string") return;
      const frame = JSON.parse(message) as {
        type: string;
        delegation?: string;
      };
      if (frame.type === "start") {
        expect(frame.delegation).toBe("synthetic-delegation");
        started = true;
        socket.send(JSON.stringify({ type: "ready" }));
      }
      if (frame.type === "end") ended = true;
    });
    saved = () => {
      socket.send(JSON.stringify({ type: "interrupt", turn: 1 }));
      socket.send(
        JSON.stringify({
          type: "ui-action",
          turn: 1,
          action: {
            type: "open_watchlist",
            movieId: 6,
            created: true,
            operationId: "1a93141a-7fe4-4210-9e46-82e2827c1db9",
          },
        }),
      );
    };
  });
  await page.goto("/your-watchlist");
  await expect.poll(() => reads).toBeGreaterThan(0);
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  await page.getByRole("button", { name: "Start voice" }).click();
  await expect.poll(() => started).toBe(true);
  const before = reads;
  saved?.();
  await expect(page).toHaveURL(/\/your-watchlist$/);
  await expect.poll(() => reads).toBeGreaterThan(before);
  await expect(page.getByText("Movie added to your watchlist.")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Undo", exact: true }),
  ).toBeVisible();
  expect(grants).toBe(1);
  expect(ended).toBe(false);
  await page.getByRole("button", { name: "End voice session" }).last().click();
  await expect.poll(() => ended).toBe(true);
});

test("personal voice changes refresh their pages and undo restores previous state", async ({
  page,
}) => {
  const user = {
    id: 7,
    username: "voice_fixture",
    email: "voice@example.invalid",
    roles: ["ROLE_USER"],
  };
  for (const url of ["**/api/v1/auth/me", "**/api/v1/accounts/me/profile"]) {
    await page.route(url, (route) => route.fulfill({ json: user }));
  }
  await page.route("**/api/v1/auth/concierge-delegation", (route) =>
    route.fulfill({
      json: {
        token: "synthetic-delegation",
        expiresAt: "2026-09-09T15:00:00Z",
      },
    }),
  );
  const reads = { ratings: 0, watchlist: 0 };
  for (const kind of ["ratings", "watchlist"] as const) {
    await page.route(`**/api/v1/accounts/**/library/${kind}**`, (route) => {
      reads[kind]++;
      return route.fulfill({
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
      });
    });
  }
  const undoRequests: { method: string; body: unknown }[] = [];
  await page.route("**/api/v1/accounts/me/ratings/6", (route) => {
    undoRequests.push({
      method: route.request().method(),
      body: route.request().postDataJSON() as unknown,
    });
    return route.fulfill({ json: {} });
  });
  await page.route("**/api/v1/accounts/me/watchlist/6", (route) => {
    undoRequests.push({ method: route.request().method(), body: null });
    return route.fulfill({ json: {} });
  });
  let emit: ((action: object) => void) | undefined;
  let ended = false;
  await page.routeWebSocket("**/v1/voice", (socket) => {
    let turn = 0;
    socket.onMessage((message) => {
      if (typeof message !== "string") return;
      const frame = JSON.parse(message) as { type: string };
      if (frame.type === "start")
        socket.send(JSON.stringify({ type: "ready" }));
      if (frame.type === "end") ended = true;
    });
    emit = (action) => {
      turn++;
      socket.send(JSON.stringify({ type: "interrupt", turn }));
      socket.send(
        JSON.stringify({
          type: "ui-action",
          turn,
          action: {
            ...action,
            movieId: 6,
            operationId: `1a93141a-7fe4-4210-9e46-${String(turn).padStart(12, "0")}`,
          },
        }),
      );
    };
  });
  await page.goto("/your-ratings");
  await expect.poll(() => reads.ratings).toBeGreaterThan(0);
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  await page.getByRole("button", { name: "Start voice" }).click();
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  const before = reads.ratings;
  emit?.({ type: "open_ratings", score: 8.5, previousScore: 7, changed: true });
  await expect(page).toHaveURL(/\/your-ratings$/);
  await expect.poll(() => reads.ratings).toBeGreaterThan(before);
  await expect(page.getByText("Your rating was saved: 8.5/10.")).toBeVisible();
  await page.getByRole("button", { name: "Undo", exact: true }).click();
  await expect.poll(() => undoRequests.length).toBe(1);
  expect(undoRequests[0]).toEqual({ method: "PUT", body: { score: 7 } });
  await expect(
    page.getByRole("button", { name: "Undo", exact: true }),
  ).toBeHidden();
  emit?.({
    type: "open_ratings",
    score: null,
    previousScore: 8.5,
    changed: true,
  });
  await expect(page.getByText("Your rating was removed.")).toBeVisible();
  await page.getByRole("button", { name: "Undo", exact: true }).click();
  await expect.poll(() => undoRequests.length).toBe(2);
  expect(undoRequests[1]).toEqual({ method: "PUT", body: { score: 8.5 } });
  await expect(
    page.getByRole("button", { name: "Undo", exact: true }),
  ).toBeHidden();
  emit?.({ type: "open_watchlist", removed: true });
  await expect(page).toHaveURL(/\/your-watchlist$/);
  await expect(
    page.getByText("Movie removed from your watchlist."),
  ).toBeVisible();
  await page.getByRole("button", { name: "Undo", exact: true }).click();
  await expect.poll(() => undoRequests.length).toBe(3);
  expect(undoRequests[2]?.method).toBe("PUT");
  await expect(
    page.getByRole("button", { name: "Undo", exact: true }),
  ).toBeHidden();
  emit?.({ type: "open_ratings", changed: false });
  await expect(page).toHaveURL(/\/your-ratings$/);
  await expect(
    page.getByText("You have no rating for this movie."),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Undo", exact: true }),
  ).toBeHidden();
  expect(ended).toBe(false);
  await page.getByRole("button", { name: "End voice session" }).last().click();
});

test("connects while the audio device starts and listens only when both are ready", async ({
  page,
}) => {
  await page.addInitScript(() => {
    let release: (() => void) | undefined;
    const resumed = new Promise<void>((resolve) => {
      release = resolve;
    });
    const state = window as typeof window & { releaseVoiceAudio: () => void };
    state.releaseVoiceAudio = () => release?.();
    const OriginalContext = window.AudioContext;
    window.AudioContext = class extends OriginalContext {
      override async resume() {
        await Promise.all([super.resume(), resumed]);
      }
    };
  });
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  let connected = false;
  let ready: (() => void) | undefined;
  await page.routeWebSocket("**/v1/voice", (socket) => {
    socket.onMessage((message) => {
      if (
        typeof message === "string" &&
        (JSON.parse(message) as { type: string }).type === "start"
      )
        connected = true;
    });
    ready = () => socket.send(JSON.stringify({ type: "ready" }));
  });
  await page.goto("/movie-search");
  await page.getByRole("button", { name: "Ask the Movie Concierge" }).click();
  await page.getByRole("button", { name: "Start voice" }).click();
  await expect.poll(() => connected).toBe(true);
  ready?.();
  await expect(page.getByRole("status")).toHaveText("Connecting…");
  await page.evaluate(() => {
    (
      window as typeof window & { releaseVoiceAudio: () => void }
    ).releaseVoiceAudio();
  });
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  await page.getByRole("button", { name: "End voice session" }).click();
});
