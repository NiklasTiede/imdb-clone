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
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await page.getByTestId("voice-lens-toggle").click();
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
  await expect(page.getByTestId("voice-lens-toggle")).toBeVisible();
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
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await page.getByTestId("voice-lens-toggle").click();
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
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await page.getByTestId("voice-lens-toggle").click();
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
  await page.goto("/your-watchlist?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await expect.poll(() => reads).toBeGreaterThan(0);
  await page.getByTestId("voice-lens-toggle").click();
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
  await page.goto("/your-ratings?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await expect.poll(() => reads.ratings).toBeGreaterThan(0);
  await page.getByTestId("voice-lens-toggle").click();
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
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await page.getByTestId("voice-lens-toggle").click();
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

test("shared voice entry retains conversation through navigation, reconnect and text", async ({
  page,
  isMobile,
}) => {
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  await page.route("**/api/v1/search/movies**", (route) =>
    route.fulfill({
      json: {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
      },
    }),
  );
  const conversationId = "1234567890abcdef1234567890abcdef";
  await page.route("**/concierge-api/v1/conversations", (route) =>
    route.fulfill({ status: 201, json: { conversationId } }),
  );
  await page.route(
    `**/concierge-api/v1/conversations/${conversationId}/messages`,
    (route) =>
      route.fulfill({
        contentType: "text/event-stream",
        body:
          `event: text\nid: 1\ndata: ${JSON.stringify({ type: "text", sequence: 1, delta: "Your typed question is here too." })}\n\n` +
          `event: completion\nid: 2\ndata: ${JSON.stringify({ type: "completion", sequence: 2, conversationId, outcome: "success" })}\n\n`,
      }),
  );
  let connections = 0;
  let ended = 0;
  let emit: (event: object) => void = () => {
    throw Error("Voice not connected");
  };
  await page.routeWebSocket("**/v1/voice", (socket) => {
    connections++;
    emit = (event) => socket.send(JSON.stringify(event));
    socket.onMessage((message) => {
      if (
        typeof message === "string" &&
        (JSON.parse(message) as { type: string }).type === "end"
      )
        ended++;
    });
    emit({ type: "ready" });
  });
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  const start = page.getByTestId("voice-lens-toggle");
  await expect(start).toHaveAttribute("data-state", "closed");
  await expect(
    page.getByRole("button", { name: "Start voice from header" }),
  ).toHaveCount(0);
  await start.click();
  await expect(
    page.getByRole("region", { name: "Voice overlay" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "What can I help you with?" }),
  ).not.toBeVisible();
  const dock = page.getByRole("region", { name: "Voice overlay" });
  const lens = await dock.locator("canvas").elementHandle();
  await page
    .getByRole("button", { name: "Expand voice conversation", exact: true })
    .click();
  const conversation = page.getByRole("complementary", {
    name: "Movie Concierge",
  });
  await expect(conversation).toBeVisible();
  await expect(dock).toBeVisible();
  await expect(page.locator("canvas")).toHaveCount(1);
  expect(await lens!.evaluate((node) => node.isConnected)).toBe(true);
  await expect(
    page.getByRole("button", { name: "End voice session", exact: true }),
  ).toHaveCount(1);
  await expect
    .poll(async () => {
      const panelBox = await conversation.boundingBox();
      const dockBox = await dock.boundingBox();
      if (!panelBox || !dockBox) return false;
      return isMobile
        ? dockBox.y >= panelBox.y + panelBox.height
        : dockBox.x + dockBox.width <= panelBox.x;
    })
    .toBe(true);
  await page
    .getByRole("button", { name: "Mute microphone", exact: true })
    .click();
  await expect(page.getByRole("status")).toHaveText("Microphone is off");
  await page
    .getByRole("button", { name: "Resume microphone", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Collapse voice conversation", exact: true })
    .click();
  await expect(conversation).not.toBeVisible();
  expect(await lens!.evaluate((node) => node.isConnected)).toBe(true);
  await page
    .getByRole("button", { name: "Expand voice conversation", exact: true })
    .click();
  await expect(conversation).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(conversation).not.toBeVisible();
  await expect(
    page.getByRole("button", {
      name: "Expand voice conversation",
      exact: true,
    }),
  ).toBeFocused();
  await page
    .getByRole("button", { name: "Expand voice conversation", exact: true })
    .click();
  expect(connections).toBe(1);
  expect(ended).toBe(0);
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  emit({ type: "interrupt", turn: 1 });
  emit({
    type: "transcript",
    turn: 1,
    speaker: "user",
    text: "Tell me about Forrest",
  });
  emit({
    type: "transcript",
    turn: 1,
    speaker: "user",
    text: "Tell me about Forrest Gump",
    final: true,
  });
  emit({
    type: "transcript",
    turn: 1,
    speaker: "assistant",
    text: "Forrest Gump stars Tom Hanks.",
    final: true,
  });
  emit({ type: "reply-complete", turn: 1 });
  await expect(
    page.getByText("Forrest Gump stars Tom Hanks.", { exact: true }),
  ).toBeVisible();
  emit({ type: "interrupt", turn: 2 });
  emit({
    type: "transcript",
    turn: 2,
    speaker: "user",
    text: "Open the homepage",
    final: true,
  });
  emit({
    type: "ui-action",
    turn: 2,
    action: { type: "open_page", destination: "home" },
  });
  await expect(page).toHaveURL(/\/$/);
  emit({ type: "interrupt", turn: 2 });
  emit({
    type: "status",
    status: "listening",
    turn: 2,
    text: "That change wasn't confirmed. Please try again.",
  });
  await expect(
    page.getByText("That change wasn't confirmed. Please try again.", {
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "End voice session", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Tell me about Forrest Gump", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("✓ Home page · open", { exact: true }),
  ).toBeVisible();
  expect(connections).toBe(1);
  expect(ended).toBe(0);
  await page
    .getByRole("button", { name: "End voice session", exact: true })
    .click();
  await expect.poll(() => ended).toBe(1);
  await page
    .getByRole("textbox", { name: "Ask the Movie Concierge", exact: true })
    .fill("Recommend a comedy");
  await page
    .getByRole("button", { name: "Send concierge message", exact: true })
    .click();
  await expect(
    page.getByText("Your typed question is here too.", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Start voice", exact: true }).click();
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  expect(connections).toBe(2);
  await expect(
    page.getByText("Forrest Gump stars Tom Hanks.", { exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", {
      name: "Start a new concierge conversation",
      exact: true,
    })
    .click();
  await expect(
    page.getByRole("heading", { name: "What can I help you with?" }),
  ).toBeVisible();
  await expect(
    page.getByText("Forrest Gump stars Tom Hanks.", { exact: true }),
  ).toHaveCount(0);
  await expect.poll(() => ended).toBe(2);
});

test("suggestions and typed follow-ups use the existing voice conversation", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  await page.route("**/api/v1/search/movies**", (route) =>
    route.fulfill({
      json: {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
      },
    }),
  );
  let textSessions = 0;
  await page.route("**/concierge-api/v1/conversations", (route) => {
    textSessions++;
    return route.fulfill({
      status: 500,
      body: "Unexpected separate text session",
    });
  });
  let connections = 0;
  let ended = 0;
  const requests: string[] = [];
  await page.routeWebSocket("**/v1/voice", (socket) => {
    connections++;
    const emit = (event: object) => socket.send(JSON.stringify(event));
    emit({ type: "ready" });
    socket.onMessage((message) => {
      if (typeof message !== "string") return;
      const command = JSON.parse(message) as { type: string; text?: string };
      if (command.type === "end") ended++;
      if (command.type !== "text" || !command.text) return;
      requests.push(command.text);
      const turn = requests.length;
      emit({ type: "interrupt", turn });
      emit({
        type: "transcript",
        speaker: "user",
        text: command.text,
        final: true,
        turn,
      });
      emit({ type: "status", status: "thinking", turn });
      emit({
        type: "transcript",
        speaker: "assistant",
        text:
          turn === 1
            ? "Here is the trailer for Forrest Gump."
            : "Robert Zemeckis directed Forrest Gump.",
        final: true,
        turn,
      });
      socket.send(Buffer.alloc(48000));
      emit({ type: "reply-complete", turn });
      emit({ type: "status", status: "listening", turn });
    });
  });
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await page.getByTestId("voice-lens-toggle").click();
  await page
    .getByRole("button", { name: "Expand voice conversation", exact: true })
    .click();
  await page.getByRole("button", { name: /Movies & trailers/ }).click();
  await expect(
    page.getByText("Here is the trailer for Forrest Gump.", { exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Mute microphone", exact: true })
    .click();
  const input = page.getByRole("textbox", {
    name: "Ask the Movie Concierge",
    exact: true,
  });
  await input.fill("Who directed it?");
  await page
    .getByRole("button", { name: "Send concierge message", exact: true })
    .click();
  await expect(input).toHaveValue("");
  await expect(
    page.getByText("Robert Zemeckis directed Forrest Gump.", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText("Who directed it?", { exact: true })).toHaveCount(
    1,
  );
  await expect(
    page.getByRole("button", { name: "Resume microphone", exact: true }),
  ).toBeVisible();
  expect(requests).toEqual([
    "Show me the trailer for Forrest Gump.",
    "Who directed it?",
  ]);
  expect(connections).toBe(1);
  expect(textSessions).toBe(0);
  expect(ended).toBe(0);
  await page
    .getByRole("button", { name: "End voice session", exact: true })
    .click();
  await expect.poll(() => ended).toBe(1);
});

test("session time limit is visible even when reading older messages", async ({
  page,
}) => {
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  await page.route("**/api/v1/search/movies**", (route) =>
    route.fulfill({
      json: {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
      },
    }),
  );
  let emit: (event: object) => void = () => {
    throw new Error("Not connected");
  };
  let connections = 0;
  await page.routeWebSocket("**/v1/voice", (socket) => {
    connections++;
    emit = (event) => socket.send(JSON.stringify(event));
    emit({ type: "ready" });
  });
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await page.getByTestId("voice-lens-toggle").click();
  await page
    .getByRole("button", { name: "Expand voice conversation", exact: true })
    .click();
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  emit({ type: "interrupt", turn: 1 });
  emit({
    type: "transcript",
    speaker: "assistant",
    text: "An earlier movie recommendation. ".repeat(60),
    final: true,
    turn: 1,
  });
  const history = page.getByRole("log", {
    name: "Movie Concierge conversation",
  });
  await expect(history).toContainText("An earlier movie recommendation.");
  await history.evaluate((element) => {
    element.scrollTop = 0;
    element.dispatchEvent(new Event("scroll"));
  });
  const message =
    "Voice session reached its 5-minute time limit. Start a new session to continue.";
  emit({ type: "error", text: message });
  await expect(page.getByRole("alert")).toHaveText(message);
  await expect(page.getByRole("alert")).toBeInViewport();
  await expect(
    page.getByRole("button", { name: "Reconnect voice", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Voice overlay" }),
  ).toBeVisible();
  await expect(page.getByTestId("voice-lens-toggle")).toHaveAttribute(
    "data-state",
    "closed",
  );
  expect(connections).toBe(1);
});

test("voice history prioritizes the spoken answer over intermediate rating results", async ({
  page,
}, testInfo) => {
  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({ status: 401, body: "" }),
  );
  await page.route("**/api/v1/search/movies**", (route) =>
    route.fulfill({
      json: {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        last: true,
      },
    }),
  );
  let emit: (event: object) => void = () => {
    throw new Error("Not connected");
  };
  await page.routeWebSocket("**/v1/voice", (socket) => {
    emit = (event) => socket.send(JSON.stringify(event));
    emit({ type: "ready" });
  });
  await page.goto("/movie-search?conciergeDebug=1");
  await page.getByRole("button", { name: "Close Movie Concierge" }).click();
  await page.getByTestId("voice-lens-toggle").click();
  await expect(page.getByRole("status")).toHaveText("Listening to you");
  await page
    .getByRole("button", { name: "Expand voice conversation", exact: true })
    .click();
  emit({ type: "interrupt", turn: 1 });
  emit({
    type: "transcript",
    turn: 1,
    speaker: "user",
    text: "Which movies have I rated highest?",
    final: true,
  });
  emit({
    type: "tool-activity",
    turn: 1,
    activity: {
      callId: "ratings-1",
      tool: "get_my_ratings",
      status: "started",
    },
  });
  // A tool call is useful debug history even before any text or movie arrives.
  const pendingEvidence = page.getByText("Tools & results", { exact: true });
  await expect(pendingEvidence).toBeVisible();
  await pendingEvidence.click();
  await expect(
    page.getByText("Read your ratings · In progress", { exact: true }),
  ).toBeVisible();
  await pendingEvidence.click();
  const titles = [
    "Arrival",
    "Forrest Gump",
    "Amelie",
    "Dune",
    "Interstellar",
    "The Dark Knight",
    "Good Will Hunting",
  ];
  for (const [i, primaryTitle] of titles.entries()) {
    emit({
      type: "movie-card",
      turn: 1,
      movie: {
        movieId: i + 1,
        primaryTitle,
        movieType: "MOVIE",
        genres: [],
        userScore: 10 - i,
        imdbRating: 7.9,
      },
    });
  }
  emit({
    type: "tool-activity",
    turn: 1,
    activity: {
      callId: "ratings-1",
      tool: "get_my_ratings",
      status: "completed",
    },
  });
  emit({
    type: "transcript",
    turn: 1,
    speaker: "assistant",
    text: "Arrival is your highest-rated movie, with ten out of ten.",
    final: true,
  });
  emit({ type: "reply-complete", turn: 1 });
  const history = page.getByRole("log", {
    name: "Movie Concierge conversation",
  });
  await expect(
    history.getByText("Which movies have I rated highest?", { exact: true }),
  ).toBeVisible();
  await expect(
    history.getByText(
      "Arrival is your highest-rated movie, with ten out of ten.",
      { exact: true },
    ),
  ).toBeVisible();
  const cards = history.getByTestId("concierge-movie-card");
  await expect(cards).toHaveCount(7);
  await expect(cards.first()).toBeHidden();
  await history.getByText("Tools & results", { exact: true }).click();
  await expect(
    history.getByText("Read your ratings · Completed", { exact: true }),
  ).toBeVisible();
  await expect(cards.first()).toContainText("Arrival");
  await expect(
    cards.first().getByText("Your rating: 10/10", { exact: true }),
  ).toBeVisible();
  await expect(
    cards.first().getByText("IMDb 7.9", { exact: true }),
  ).toBeVisible();
  await expect(cards.last()).toContainText("Good Will Hunting");
  await page.screenshot({
    path: testInfo.outputPath("voice-history-results.png"),
  });
  await history.getByText("Tools & results", { exact: true }).click();
  await expect(cards.first()).toBeHidden();
  await expect(
    history.getByText(
      "Arrival is your highest-rated movie, with ten out of ten.",
      { exact: true },
    ),
  ).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("voice-history.png") });
});
