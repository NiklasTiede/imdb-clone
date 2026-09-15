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

test.beforeEach(async ({ page }) => {
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
});

test("one lens starts, closes on inactivity, restarts and ends voice without exposing history", async ({
  page,
}, testInfo) => {
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  let microphoneRequests = 0;
  await page.exposeFunction("microphoneRequested", () => {
    microphoneRequests++;
  });
  await page.addInitScript(() => {
    const original = navigator.mediaDevices.getUserMedia.bind(
      navigator.mediaDevices,
    );
    navigator.mediaDevices.getUserMedia = (constraints) => {
      void (
        window as unknown as { microphoneRequested: () => Promise<void> }
      ).microphoneRequested();
      return original(constraints);
    };
  });
  let connections = 0;
  let endings = 0;
  let emit: (event: object) => void = () => {
    throw new Error("Not connected");
  };
  await page.routeWebSocket("**/v1/voice", (socket) => {
    connections++;
    emit = (event) => socket.send(JSON.stringify(event));
    emit({ type: "ready" });
    socket.onMessage((message) => {
      if (typeof message === "string") {
        const command = JSON.parse(message) as { type: string };
        if (command.type === "end") endings++;
      }
    });
  });
  await page.goto("/movie-search");
  const lens = page.getByTestId("voice-lens-toggle");
  const drawer = page.getByRole("complementary", { name: "Movie Concierge" });
  await expect(lens).toHaveAccessibleName("Start voice");
  await expect(lens).toHaveAttribute("data-state", "closed");
  await expect(drawer).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Start voice from header" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("button", {
      name: /Expand voice conversation|Open voice conversation|Mute microphone/,
    }),
  ).toHaveCount(0);
  expect(pageErrors).toEqual([]);
  expect(microphoneRequests).toBe(0);
  expect(connections).toBe(0);
  const box = await lens.boundingBox();
  expect(box).not.toBeNull();
  expect(
    Math.abs(box!.x + box!.width / 2 - page.viewportSize()!.width / 2),
  ).toBeLessThan(2);
  await expect(
    page.getByRole("heading", { name: "Search movies" }),
  ).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("lens-closed.png") });
  await lens.click();
  await expect(lens).toHaveAttribute("data-state", "listening");
  await expect(lens).toHaveAccessibleName("End voice session");
  await expect.poll(() => microphoneRequests).toBe(1);
  await expect(
    page.getByRole("region", { name: "Voice overlay" }).getByRole("button"),
  ).toHaveCount(1);
  emit({
    type: "transcript",
    speaker: "user",
    text: "Tell me about this movie",
    final: true,
    turn: 1,
  });
  emit({
    type: "transcript",
    speaker: "assistant",
    text: "A private session transcript",
    final: true,
    turn: 1,
  });
  await expect(
    page.getByText("A private session transcript", { exact: true }),
  ).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath("lens-active.png") });
  emit({ type: "standby" });
  await expect(lens).toHaveAttribute("data-state", "closed");
  await expect(lens).toHaveAccessibleName("Resume voice");
  await expect(drawer).toHaveCount(0);
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect.poll(() => endings).toBe(1);
  await lens.click();
  await expect(lens).toHaveAttribute("data-state", "listening");
  expect(connections).toBe(2);
  emit({
    type: "error",
    text: "Voice session reached its 5-minute time limit. Start a new session to continue.",
  });
  await expect(page.getByRole("alert")).toContainText("5-minute time limit");
  await expect(lens).toHaveAccessibleName("Reconnect voice");
  await expect(drawer).toHaveCount(0);
  await lens.click();
  await expect(lens).toHaveAttribute("data-state", "listening");
  await expect(page.getByRole("alert")).toHaveCount(0);
  await lens.press("Enter");
  await expect(lens).toHaveAccessibleName("Start voice");
  await expect.poll(() => endings).toBe(3);
  expect(pageErrors).toEqual([]);
  await expect(drawer).toHaveCount(0);
});

test("microphone denial provides help without opening a conversation", async ({
  page,
}) => {
  await page.addInitScript(() => {
    navigator.mediaDevices.getUserMedia = () =>
      Promise.reject(new DOMException("Denied", "NotAllowedError"));
  });
  await page.goto("/movie-search");
  await page.getByTestId("voice-lens-toggle").click();
  await expect(page.getByRole("alert")).toContainText(
    "Microphone access was denied",
  );
  await expect(page.getByTestId("voice-lens-toggle")).toHaveAttribute(
    "data-state",
    "closed",
  );
  await expect(
    page.getByRole("complementary", { name: "Movie Concierge" }),
  ).toHaveCount(0);
});

test("connecting can be cancelled from the same lens", async ({ page }) => {
  await page.routeWebSocket("**/v1/voice", () => {});
  await page.goto("/movie-search");
  const lens = page.getByTestId("voice-lens-toggle");
  await lens.click();
  await expect(lens).toHaveAccessibleName("Cancel voice connection");
  await lens.click();
  await expect(lens).toHaveAccessibleName("Start voice");
  await expect(lens).toHaveAttribute("data-state", "closed");
});

test("closed lens stops painting, wakes on resize and animates during voice", async ({
  page,
}) => {
  await page.addInitScript(() => {
    const counter = { frames: 0 };
    (window as unknown as { lensPaints: typeof counter }).lensPaints = counter;
    // eslint-disable-next-line @typescript-eslint/unbound-method -- Rebound to the drawing context with apply below.
    const clear = CanvasRenderingContext2D.prototype.clearRect;
    CanvasRenderingContext2D.prototype.clearRect = function (...args) {
      if (this.canvas.closest('[data-testid="voice-lens-toggle"]'))
        counter.frames++;
      clear.apply(this, args);
    };
  });
  await page.routeWebSocket("**/v1/voice", (socket) => {
    socket.send(JSON.stringify({ type: "ready" }));
  });
  await page.goto("/movie-search");
  const lens = page.getByTestId("voice-lens-toggle");
  await expect(lens).toBeVisible();
  const paintedFrames = () =>
    page.evaluate(
      () =>
        (window as unknown as { lensPaints: { frames: number } }).lensPaints
          .frames,
    );
  const paintDuringSample = () =>
    page.evaluate(async () => {
      const counter = (window as unknown as { lensPaints: { frames: number } })
        .lensPaints;
      const before = counter.frames;
      await new Promise((resolve) => setTimeout(resolve, 250));
      return counter.frames - before;
    });
  await expect.poll(paintedFrames).toBeGreaterThan(0);
  await expect.poll(paintDuringSample).toBe(0);
  const beforeResize = await paintedFrames();
  await lens.locator("canvas").evaluate((canvas) => {
    canvas.style.width = "100px";
  });
  await expect.poll(paintedFrames).toBeGreaterThan(beforeResize);
  await expect.poll(paintDuringSample).toBe(0);
  await lens.click();
  await expect(lens).toHaveAttribute("data-state", "listening");
  await expect.poll(paintDuringSample).toBeGreaterThan(0);
  await lens.click();
  await expect(lens).toHaveAttribute("data-state", "closed");
  await expect.poll(paintDuringSample, { timeout: 10000 }).toBe(0);
});
