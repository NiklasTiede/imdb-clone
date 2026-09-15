import { expect, test } from "@playwright/test";
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { z } from "zod";
import { startBackend } from "./backend";
import { installAudioLab, snapshot } from "./browser";
import { hasFreshMovieAction, saveEvidence, WireEvidence } from "./evidence";

const enabled = process.env.VOICE_STRESS_LIVE === "1";
const profile = process.env.VOICE_STRESS_PROFILE ?? "stress";
const profiles = {
  rapid: { seconds: 60, tasks: 1 },
  quick: { seconds: 90, tasks: 3 },
  stress: { seconds: 240, tasks: 10 },
  soak: { seconds: 285, tasks: 12 },
};
if (!(profile in profiles))
  throw new Error("VOICE_STRESS_PROFILE must be rapid, quick, stress or soak");
const limits = profiles[profile as keyof typeof profiles];
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
const movieSchema = z.object({
  content: z.array(
    z.object({
      id: z.number().int().positive(),
      primaryTitle: z.string(),
      trailerYoutubeKey: z.string().nullable().optional(),
    }),
  ),
});

for (const model of ["grok", "gpt-live-1"] as const) {
  test(`live ${model}: repeated movie tasks, spoken interruption and rapid follow-ups`, async ({
    page,
    request,
  }, info) => {
    test.skip(
      !enabled,
      "Paid provider test; explicitly set VOICE_STRESS_LIVE=1",
    );
    const expected = new Map<string, number>();
    for (const title of ["Forrest Gump", "Interstellar", "Arrival"]) {
      const response = await request.post(
        `http://localhost:8080/api/v1/search/movies?query=${encodeURIComponent(title)}&size=10`,
        { data: {} },
      );
      expect(
        response.ok(),
        "Local Java catalog must be available before any paid session",
      ).toBe(true);
      const movie = movieSchema
        .parse(await response.json())
        .content.find((entry) => entry.primaryTitle === title);
      expect(movie, `Missing fixture title ${title}`).toBeDefined();
      expected.set(title, movie!.id);
      if (title !== "Arrival")
        expect(
          movie!.trailerYoutubeKey,
          `Missing trailer for ${title}`,
        ).toBeTruthy();
    }
    const fixtureNames = [
      "open-forrest",
      "trailer-forrest",
      "open-interstellar",
      "trailer-interstellar",
      "open-arrival",
      "details",
      "correction",
    ];
    const fixtures = new Map<string, string>();
    for (const name of fixtureNames)
      fixtures.set(
        name,
        (
          await readFile(
            path.resolve(`../agent/evals/voice/stress/${name}.wav`),
          )
        ).toString("base64"),
      );
    // All fixtures and catalog preconditions are checked before starting a billable connection.
    const backend = await startBackend();
    const wire = new WireEvidence(
      page,
      new Map([...expected].map(([title, id]) => [id, title])),
    );
    const outcomes: object[] = [];
    let started = 0;
    let hardStop: ReturnType<typeof setTimeout> | undefined;
    let passed = 0;
    let attempted = 0;
    let deadlineReached = false;
    try {
      await installAudioLab(page);
      // Redirect the actual browser WebSocket, without a Node audio relay or protocol mock.
      await page.addInitScript(() => {
        const Original = window.WebSocket;
        window.WebSocket = class extends Original {
          constructor(url: string | URL, protocols?: string | string[]) {
            const target = new URL(url, location.href);
            super(
              target.pathname.endsWith("/v1/voice")
                ? "ws://127.0.0.1:8096/v1/voice"
                : url,
              protocols,
            );
          }
        };
      });
      await page.route("**/v1/voice/models", async (route) => {
        const response = await request.get(
          "http://127.0.0.1:8096/v1/voice/models",
        );
        await route.fulfill({ response });
      });
      // This suite is anonymous. Never import the developer's authenticated storage state.
      await page.goto("/movie-search");
      if (model === "gpt-live-1") {
        await page.getByRole("button", { name: "Voice model: Grok" }).click();
        await page.getByRole("menuitem", { name: "GPT-Live 1" }).click();
      }
      started = Date.now();
      expect(
        backend.catalogInterrupted,
        "Catalog failed during test startup",
      ).toBe(false);
      hardStop = setTimeout(() => {
        deadlineReached = true;
        void backend.stop();
      }, limits.seconds * 1000);
      await page.getByTestId("voice-lens-toggle").click();
      await expect
        .poll(() => wire.events.some((event) => event.type === "ready"), {
          timeout: 20_000,
        })
        .toBe(true);
      const speak = async (name: string) => {
        const interval = await page.evaluate(
          (wav) => window.voiceStress.speak(wav),
          fixtures.get(name)!,
        );
        return interval;
      };
      const waitUntil = async (
        predicate: () => Promise<boolean>,
        milliseconds: number,
      ) => {
        const end = Math.min(
          started + limits.seconds * 1000 - 2000,
          Date.now() + milliseconds,
        );
        while (Date.now() < end && !deadlineReached) {
          if (backend.catalogInterrupted) return false;
          if (await predicate()) return true;
          if (
            wire.events.some(
              (event) => event.type === "error" || event.type === "standby",
            )
          )
            return false;
          await delay(100);
        }
        return false;
      };
      for (
        let index = 0;
        index < limits.tasks &&
        Date.now() < started + limits.seconds * 1000 - 20_000;
        index++
      ) {
        if (
          deadlineReached ||
          backend.catalogInterrupted ||
          wire.events.some(
            (event) => event.type === "error" || event.type === "standby",
          )
        )
          break;
        const mode =
          profile === "rapid"
            ? "rapid-follow-ups"
            : index % 4 === 2
              ? "spoken-interruption"
              : index % 4 === 3
                ? "rapid-follow-ups"
                : "normal";
        const title =
          mode === "rapid-follow-ups"
            ? "Arrival"
            : mode === "spoken-interruption"
              ? "Interstellar"
              : index % 2
                ? "Forrest Gump"
                : index % 3 === 0
                  ? "Arrival"
                  : "Forrest Gump";
        const trailer = mode === "normal" && index % 2 === 1;
        const requestStart = Date.now();
        let overlapObserved = false;
        if (mode === "spoken-interruption") {
          const details = await speak("details");
          overlapObserved = await waitUntil(
            () =>
              page.evaluate((end) => {
                const frame = window.voiceStress.rendered.at(-1);
                return (
                  !!frame &&
                  frame.at > end &&
                  frame.samples.some((sample) => Math.abs(sample) > 1000)
                );
              }, details.end),
            12_000,
          );
        }
        if (mode === "rapid-follow-ups") {
          await speak("open-forrest");
          await delay(100);
        }
        if (
          wire.events.some(
            (event) => event.type === "error" || event.type === "standby",
          )
        )
          break;
        const actionStart = Date.now();
        const spoken = await speak(
          mode === "spoken-interruption"
            ? "correction"
            : mode === "rapid-follow-ups"
              ? "open-arrival"
              : trailer
                ? "trailer-forrest"
                : title === "Arrival"
                  ? "open-arrival"
                  : "open-forrest",
        );
        attempted++;
        const inputEnd = Date.now();
        const movieId = expected.get(title)!;
        const hasFreshAction = () =>
          hasFreshMovieAction(wire.events, actionStart, title, trailer);
        const correctPage = await waitUntil(async () => {
          if (!hasFreshAction()) return false;
          const url = new URL(page.url());
          if (
            url.pathname !== "/movie" ||
            url.searchParams.get("id") !== String(movieId) ||
            url.hash !== (trailer ? "#trailer" : "")
          )
            return false;
          if (
            !(await page
              .getByRole("heading", { name: title, exact: true, level: 1 })
              .isVisible())
          )
            return false;
          if (trailer) {
            const region = page.getByRole("region", {
              name: "Movie trailer section",
            });
            const rect = await region.boundingBox();
            return (
              !!rect &&
              Math.abs(
                rect.y +
                  rect.height / 2 -
                  (page.viewportSize()?.height ?? 0) / 2,
              ) < 8
            );
          }
          return true;
        }, 22_000);
        const taskDoneAt = Date.now();
        const firstRendered = await waitUntil(
          () =>
            page.evaluate(
              (end) =>
                window.voiceStress.rendered.some(
                  (frame) =>
                    frame.at > end &&
                    frame.samples.some((sample) => Math.abs(sample) > 1000),
                ),
              spoken.end,
            ),
          10_000,
        );
        const freshAction = hasFreshAction();
        const succeeded =
          correctPage &&
          freshAction &&
          firstRendered &&
          (mode !== "spoken-interruption" || overlapObserved);
        if (succeeded) passed++;
        const firstAudioMs = await page.evaluate((end) => {
          const frame = window.voiceStress.rendered.find(
            (entry) =>
              entry.at > end &&
              entry.samples.some((sample) => Math.abs(sample) > 1000),
          );
          return frame ? frame.at - end : null;
        }, spoken.end);
        outcomes.push({
          index,
          actionStart,
          mode,
          fixtureTitle: title,
          trailer,
          overlapObserved,
          correctPage,
          observedPage: {
            pathname: new URL(page.url()).pathname,
            movieMatches:
              new URL(page.url()).searchParams.get("id") === String(movieId),
            trailerHash: new URL(page.url()).hash === "#trailer",
            headingVisible: await page
              .getByRole("heading", { name: title, exact: true, level: 1 })
              .isVisible(),
          },
          freshAction,
          firstRendered,
          passed: succeeded,
          taskMs: taskDoneAt - inputEnd,
          firstRenderedAfterInputMs: firstAudioMs,
          latencyAttribution:
            mode === "normal"
              ? "may include previous reply tail; review recording"
              : "overlap diagnostic, not attributable response latency",
          tools: wire.events.filter(
            (event) =>
              event.at >= requestStart && event.type === "tool-activity",
          ),
        });
        // Longer sessions keep the same socket; no fresh conversation per assertion.
        await delay(profile === "soak" ? 4000 : 1000);
      }
    } finally {
      clearTimeout(hardStop);
      // Exporting several minutes of PCM can block Node's event loop. A TCP timeout
      // during that export is not evidence of a catalog outage during the conversation.
      backend.stopMonitoring();
      let browser: Awaited<ReturnType<typeof snapshot>> = {
        timeOrigin: 0,
        sampleRate: 48000,
        playback: [],
        rendered: [],
        microphone: [],
      };
      let snapshotAvailable = false;
      try {
        if (!page.isClosed()) {
          const lens = page.getByTestId("voice-lens-toggle");
          if (
            (await lens.count()) &&
            (await lens.getAttribute("aria-label", { timeout: 1000 })) ===
              "End voice session"
          )
            await lens.click({ timeout: 1000 });
          await delay(500);
          browser = await snapshot(page);
          snapshotAvailable = true;
        }
      } catch {
        // A vanished app or browser crash must not suppress provider/wire diagnostics.
      } finally {
        await backend.stop();
      }
      await saveEvidence(info, browser, wire, {
        mode: "live",
        model,
        profile,
        limits,
        elapsedSeconds: started ? (Date.now() - started) / 1000 : 0,
        deadlineReached,
        catalogInterrupted: backend.catalogInterrupted,
        attempted,
        passed,
        expectedTasks: limits.tasks,
        completionRate: passed / limits.tasks,
        snapshotAvailable,
        outcomes,
      });
      const file = info.outputPath("backend.json");
      await writeFile(file, JSON.stringify(backend.logs, null, 2));
      await info.attach("backend.json", {
        path: file,
        contentType: "application/json",
      });
    }
    expect(
      backend.catalogInterrupted,
      "Local catalog went offline; this run cannot establish provider reliability",
    ).toBe(false);
    expect(
      attempted,
      "Session ended or time cap reached before all scheduled tasks",
    ).toBe(limits.tasks);
    expect(
      passed,
      "Inspect report.json and both WAV files for failed tasks",
    ).toBe(limits.tasks);
  });
}
