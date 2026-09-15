import { expect, test } from "@playwright/test";
import { installAudioLab, snapshot } from "./browser";
import {
  hasFreshMovieAction,
  saveEvidence,
  signalMetrics,
  WireEvidence,
} from "./evidence";

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
function tone(milliseconds: number, frequency = 440) {
  const buffer = Buffer.alloc(milliseconds * 48);
  for (let i = 0; i < buffer.length / 2; i++)
    buffer.writeInt16LE(
      Math.round(7000 * Math.sin((2 * Math.PI * frequency * i) / 24000)),
      i * 2,
    );
  return buffer;
}

test("diagnostics identify clipping and sustained tonal candidates without judging naturalness", () => {
  expect(signalMetrics(Array(24000).fill(32767), 24000).clippingRatio).toBe(1);
  const sinusoid = Array.from({ length: 24000 }, (_, i) =>
    Math.round(10000 * Math.sin((2 * Math.PI * 2000 * i) / 24000)),
  );
  expect(
    signalMetrics(sinusoid, 24000).longestTonalCandidateMs,
  ).toBeGreaterThan(500);
  expect(
    signalMetrics(Array(24000).fill(0), 24000).listeningReviewRequired,
  ).toBe(false);
});

for (const model of ["grok", "gpt-live-1"] as const) {
  test(`deterministic ${model}: early greeting, bursts, jitter, repeated interruption and navigation`, async ({
    page,
  }, info) => {
    await installAudioLab(page, 1200);
    await page.route("**/api/v1/**", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.includes("/auth/me"))
        return route.fulfill({ status: 401 });
      const movie = /\/movies\/(\d+)$/.exec(url.pathname);
      return route.fulfill({
        json: movie
          ? {
              id: Number(movie[1]),
              primaryTitle: "Forrest Gump",
              movieType: "MOVIE",
              movieGenre: ["DRAMA"],
              trailerYoutubeKey: "abcDEF123_-",
              description: "Synthetic catalog",
            }
          : {
              content: [],
              items: [],
              page: 0,
              size: 20,
              totalElements: 0,
              totalPages: 0,
              last: true,
            },
      });
    });
    await page.route("**/v1/voice/models", (route) =>
      route.fulfill({ json: { models: ["grok", "gpt-live-1"] } }),
    );
    const wire = new WireEvidence(undefined);
    let send: (payload: string | Buffer) => void = () => {
      throw new Error("No socket");
    };
    let inputBytes = 0;
    await page.routeWebSocket("**/v1/voice", (socket) => {
      wire.connections++;
      send = (payload) => {
        wire.receive(payload);
        socket.send(payload);
      };
      socket.onMessage((payload) => {
        if (typeof payload !== "string") {
          inputBytes += payload.length;
          wire.inputBytes += payload.length;
          return;
        }
        const event = JSON.parse(payload) as { type: string };
        if (event.type === "start") {
          send(JSON.stringify({ type: "ready" }));
          for (let i = 0; i < 4; i++) send(tone(100));
          send(JSON.stringify({ type: "reply-complete", turn: 0 }));
        }
      });
    });
    const errors: string[] = [];
    page.on("pageerror", (error) => errors.push(error.name));
    await page.goto("/movie-search");
    if (model === "gpt-live-1") {
      await page.getByRole("button", { name: "Voice model: Grok" }).click();
      await page.getByRole("menuitem", { name: "GPT-Live 1" }).click();
    }
    await page.getByTestId("voice-lens-toggle").click();
    try {
      await expect.poll(() => inputBytes).toBeGreaterThan(960);
      await expect
        .poll(() =>
          page.evaluate(
            () =>
              window.voiceStress.playback.filter((e) => e.type === "scheduled")
                .length,
          ),
        )
        .toBe(4);
      for (let turn = 1; turn <= 12; turn++) {
        const emit = (event: object) => send(JSON.stringify(event));
        // Multiple queued sources are cancelled, then the latest answer remains playable.
        send(tone(300));
        send(tone(300));
        emit({ type: "interrupt", turn });
        emit({
          type: "tool-activity",
          turn,
          activity: {
            callId: `call-${turn}`,
            tool: "search_movies",
            status: "started",
          },
        });
        for (const gap of [0, 20, turn === 6 ? 350 : 90, 0]) {
          await delay(gap);
          send(tone(100));
        }
        emit({
          type: "movie-card",
          turn,
          movie: {
            movieId: turn,
            primaryTitle: "Forrest Gump",
            movieType: "MOVIE",
            genres: [],
          },
        });
        emit({
          type: "tool-activity",
          turn,
          activity: {
            callId: `call-${turn}`,
            tool: "search_movies",
            status: "completed",
          },
        });
        const action = {
          type: turn % 2 ? "open_movie" : "open_movie_trailer",
          movieId: turn,
        };
        emit({ type: "ui-action", turn, action });
        emit({ type: "ui-action", turn, action });
        emit({ type: "reply-complete", turn });
        await expect(page).toHaveURL(
          `/movie?id=${turn}${turn % 2 ? "" : "#trailer"}`,
        );
        if (turn % 2 === 0)
          await expect(
            page.getByRole("region", { name: "Movie trailer section" }),
          ).toBeFocused();
        await expect(page.getByTestId("voice-lens-toggle")).toBeVisible();
      }
      // Two grounded movie commands may belong to one VAD speech turn. A late
      // duplicate of the first destination must not undo the newer destination.
      send(
        JSON.stringify({
          type: "movie-card",
          turn: 12,
          movie: {
            movieId: 42,
            primaryTitle: "Forrest Gump",
            movieType: "MOVIE",
            genres: [],
          },
        }),
      );
      send(
        JSON.stringify({
          type: "ui-action",
          turn: 12,
          action: { type: "open_movie", movieId: 42 },
        }),
      );
      send(
        JSON.stringify({
          type: "ui-action",
          turn: 12,
          action: { type: "open_movie_trailer", movieId: 12 },
        }),
      );
      await expect(page).toHaveURL("/movie?id=42");
      expect(errors).toEqual([]);
      const evidence = await snapshot(page);
      expect(
        evidence.playback.filter((event) => event.type === "stopped").length,
      ).toBeGreaterThan(12);
      expect(
        signalMetrics(
          evidence.rendered.flatMap((frame) => frame.samples),
          evidence.sampleRate,
        ).rms,
      ).toBeGreaterThan(0.001);
    } finally {
      await page.getByTestId("voice-lens-toggle").click();
      await saveEvidence(info, await snapshot(page), wire, {
        mode: "deterministic",
        model,
        tasks: 12,
      });
    }
  });
}

test("outcomes require a new matching action and input diagnostics distinguish silence", () => {
  const action = {
    at: 100,
    type: "ui-action",
    action: "open_movie",
    matchedMovie: "Arrival",
  };
  expect(hasFreshMovieAction([action], 101, "Arrival", false)).toBe(false);
  expect(hasFreshMovieAction([action], 100, "Arrival", true)).toBe(false);
  expect(hasFreshMovieAction([action], 100, "Forrest Gump", false)).toBe(false);
  expect(hasFreshMovieAction([action], 100, "Arrival", false)).toBe(true);
  const wire = new WireEvidence(undefined);
  wire.recordInput(Buffer.alloc(960));
  wire.recordInput(tone(20));
  expect(wire.inputSignal[0]!.peak).toBe(0);
  expect(wire.inputSignal[1]!.peak).toBeGreaterThan(0.2);
  expect(wire.inputBytes).toBe(1920);
});
